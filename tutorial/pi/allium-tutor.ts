import { copyFileSync, existsSync, readFileSync, writeFileSync } from "node:fs";
import { basename, join } from "node:path";
import type { ExtensionAPI, ExtensionCommandContext, ExtensionContext } from "@earendil-works/pi-coding-agent";
import { Key, matchesKey, truncateToWidth, wrapTextWithAnsi } from "@earendil-works/pi-tui";

const ACTIONS = [
	"1. Start here: what are we doing?",
	"2. Run the first check and see the expected failure",
	"3. Ask Pi to explain the TODO slowly, without editing",
	"4. Edit the missing rule yourself",
	"5. Check my work",
	"6. Show a hint",
	"7. Let Pi make the smallest safe edit",
	"8. Compare with the answer",
	"9. Reset the starter file",
	"Exit",
];

function envPath(name: string, fallback: string): string {
	return process.env[name] || fallback;
}

function lessonId(): string {
	return process.env.ALLIUM_TUTOR_LESSON_ID || "01-first-rule";
}

function workspace(ctx: ExtensionContext): string {
	return envPath("ALLIUM_TUTOR_WORKSPACE", ctx.cwd);
}

function starterPath(ctx: ExtensionContext): string {
	return envPath("ALLIUM_TUTOR_STARTER", join(workspace(ctx), "library-starter.allium"));
}

function solutionPath(ctx: ExtensionContext): string {
	return envPath("ALLIUM_TUTOR_SOLUTION", join(workspace(ctx), "library-solution.allium"));
}

function lessonMarkdownPath(ctx: ExtensionContext): string {
	return envPath("ALLIUM_TUTOR_LESSON_MD", join(workspace(ctx), "README.md"));
}

function fixturePath(ctx: ExtensionContext): string {
	const root = process.env.ALLIUM_TUTOR_REPO_ROOT || ctx.cwd;
	return envPath("ALLIUM_TUTOR_FIXTURE", join(root, "tutorial", "fixtures", lessonId(), "library-starter.allium"));
}

function setTutorWidget(ctx: ExtensionContext): void {
	const file = basename(starterPath(ctx));
	ctx.ui.setStatus("allium-tutor", ctx.ui.theme.fg("accent", `Allium Tutor ${lessonId()}`));
	ctx.ui.setWidget(
		"allium-tutor",
		[
			ctx.ui.theme.fg("accent", "Allium Tutor — Lesson 01"),
			`Focus file: ${file}`,
			"Ignore the rest of Pi for now. Use /allium-tutor and take one small step at a time.",
			"Loop: understand → check → edit → check again.",
		],
		{ placement: "aboveEditor" },
	);
}

async function showCard(ctx: ExtensionCommandContext, title: string, body: string): Promise<void> {
	if (ctx.mode !== "tui") {
		ctx.ui.notify(`${title}\n\n${body}`, "info");
		return;
	}

	await ctx.ui.custom<void>((tui, theme, _keybindings, done) => {
		let cachedWidth: number | undefined;
		let cachedLines: string[] | undefined;

		function build(width: number): string[] {
			const contentWidth = Math.max(30, width - 4);
			const lines: string[] = [];
			const border = theme.fg("border", "─".repeat(Math.max(10, Math.min(width, 100))));

			lines.push(border);
			lines.push(theme.fg("accent", theme.bold(title)));
			lines.push(theme.fg("dim", "Press Enter, q, or Escape to return to the tutor menu."));
			lines.push(border);
			lines.push("");

			for (const rawLine of body.split("\n")) {
				if (rawLine.trim() === "") {
					lines.push("");
					continue;
				}

				const line = rawLine.startsWith("    ") || rawLine.startsWith("$") ? theme.fg("mdCode", rawLine) : rawLine;
				for (const wrapped of wrapTextWithAnsi(line, contentWidth)) {
					lines.push(wrapped);
				}
			}

			lines.push("");
			lines.push(border);
			lines.push(theme.fg("dim", "Enter/q/Escape: back to menu"));

			return lines.map((line) => truncateToWidth(line, width, ""));
		}

		return {
			render(width: number) {
				if (cachedLines && cachedWidth === width) return cachedLines;
				cachedWidth = width;
				cachedLines = build(width);
				return cachedLines;
			},
			handleInput(data: string) {
				if (matchesKey(data, Key.enter) || matchesKey(data, Key.escape) || data === "q" || data === "Q") {
					done(undefined);
					return;
				}
				tui.requestRender();
			},
			invalidate() {
				cachedWidth = undefined;
				cachedLines = undefined;
			},
		};
	});
}

async function showLessonCard(ctx: ExtensionCommandContext): Promise<void> {
	await showCard(
		ctx,
		"Lesson 01 — First Rule",
		[
			"You are adding exactly one behavior rule to a copied starter file.",
			"",
			"Plain-English behavior:",
			"When a member borrows an available book, the book becomes borrowed, records the borrower, receives a due date, and emits a BookBorrowed fact.",
			"",
			"Target file:",
			`    ${basename(starterPath(ctx))}`,
			"",
			"Why the interface may look busy:",
			"This is still Pi. The tutor is just guiding Pi. Ignore the session header/footer for now and choose one tutor menu step at a time.",
			"",
			"Recommended path:",
			"1. Run the first check and see the expected failure.",
			"2. Ask Pi to explain the TODO without editing.",
			"3. Edit the missing BorrowBook rule yourself, or let Pi make the smallest safe edit.",
			"4. Check again until all diagnostics and findings are clean.",
			"",
			`Full lesson file copied to: ${lessonMarkdownPath(ctx)}`,
		].join("\n"),
	);
}

async function editStarter(ctx: ExtensionCommandContext): Promise<void> {
	const path = starterPath(ctx);
	if (!existsSync(path)) {
		ctx.ui.notify(`Missing starter: ${path}`, "error");
		return;
	}

	const before = readFileSync(path, "utf8");
	const edited = await ctx.ui.editor("REAL EDIT — library-starter.allium (Enter saves, Escape cancels)", before);
	if (edited === undefined || edited === before) {
		ctx.ui.notify("No changes saved", "info");
		return;
	}

	const ok = await ctx.ui.confirm("Save starter changes?", `Overwrite ${path}?`);
	if (!ok) {
		ctx.ui.notify("Changes discarded", "info");
		return;
	}

	writeFileSync(path, edited, "utf8");
	ctx.ui.notify("Saved. Next step: choose `Check my work`.", "info");
}

function summarizeAllium(label: string, result: { stdout?: string; stderr?: string; code?: number | null; killed?: boolean }): string {
	const status = result.code === 0 ? "✅" : "⚠️";
	const lines = [`${status} $ ${label}`, `exit code: ${result.code ?? "unknown"}${result.killed ? " (killed)" : ""}`];
	const stdout = (result.stdout || "").trim();

	if (stdout) {
		try {
			const payload = JSON.parse(stdout) as {
				diagnostics?: Array<{ code?: string; message?: string; severity?: string; location?: { line?: number; col?: number } }>;
				findings?: Array<{ type?: string; message?: string; severity?: string }>;
			};
			const diagnostics = payload.diagnostics || [];
			const findings = payload.findings || [];

			if (diagnostics.length === 0 && findings.length === 0) {
				lines.push("No diagnostics. No findings. This gate is clean.");
			} else {
				if (diagnostics.length > 0) {
					lines.push("Diagnostics:");
					for (const diagnostic of diagnostics) {
						const where = diagnostic.location?.line ? ` line ${diagnostic.location.line}` : "";
						lines.push(`- ${diagnostic.severity || "diagnostic"} ${diagnostic.code || "unknown"}${where}: ${diagnostic.message || ""}`);
					}
				}
				if (findings.length > 0) {
					lines.push("Findings:");
					for (const finding of findings) {
						lines.push(`- ${finding.severity || "finding"} ${finding.type || "unknown"}: ${finding.message || ""}`);
					}
				}
			}
		} catch {
			lines.push(stdout);
		}
	}

	if (result.stderr?.trim()) {
		lines.push("stderr:", result.stderr.trim());
	}

	return lines.join("\n");
}

async function runAllium(pi: ExtensionAPI, ctx: ExtensionCommandContext): Promise<void> {
	const target = basename(starterPath(ctx));
	const outputs: string[] = [];

	for (const args of [
		["check", target],
		["analyse", target],
	] as const) {
		const label = `allium ${args.join(" ")}`;
		try {
			const result = await pi.exec("allium", [...args], { timeout: 10_000 });
			outputs.push(summarizeAllium(label, result));
		} catch (error) {
			outputs.push(`⚠️ $ ${label}\nerror: ${error instanceof Error ? error.message : String(error)}`);
		}
	}

	await showCard(
		ctx,
		"Check result",
		[
			"This is the deterministic gate. Green means the spec is structurally and semantically clean enough for this lesson.",
			"",
			outputs.join("\n\n---\n\n"),
			"",
			"If you still see `borrowed is never assigned`, that is expected before adding BorrowBook. Add the missing rule, then check again.",
		].join("\n"),
	);
}

async function showHint(ctx: ExtensionCommandContext): Promise<void> {
	await showCard(
		ctx,
		"Hint — shape of the missing rule",
		[
			"Add this rule where the TODO appears:",
			"",
			"    rule BorrowBook {",
			"        when: MemberBorrowsBook(member)",
			"        requires: book.status = on_shelf",
			"        ensures: book.status = borrowed",
			"        ensures: book.borrowed_by = member.name",
			"        ensures: book.due_at = now + config.loan_period",
			"        ensures: BookBorrowed(book: book, member: member)",
			"    }",
			"",
			"Read it as: event → case guard → state change → attribution → deadline → emitted fact.",
		].join("\n"),
	);
}

async function showAnswer(ctx: ExtensionCommandContext): Promise<void> {
	await showCard(
		ctx,
		"Answer comparison — the missing rule",
		[
			"Your rule does not have to be beautiful. For this lesson, it should make this behavior explicit:",
			"",
			"    rule BorrowBook {",
			"        when: MemberBorrowsBook(member)",
			"        requires: book.status = on_shelf",
			"        ensures: book.status = borrowed",
			"        ensures: book.borrowed_by = member.name",
			"        ensures: book.due_at = now + config.loan_period",
			"        ensures: BookBorrowed(book: book, member: member)",
			"    }",
			"",
			`Full solution file: ${solutionPath(ctx)}`,
			"",
			"After comparing, choose `Check my work` again.",
		].join("\n"),
	);
}

async function resetStarter(ctx: ExtensionCommandContext): Promise<void> {
	const fixture = fixturePath(ctx);
	const starter = starterPath(ctx);
	if (!existsSync(fixture)) {
		ctx.ui.notify(`Missing fixture: ${fixture}`, "error");
		return;
	}
	const ok = await ctx.ui.confirm("Reset lesson file?", `Replace ${starter} with the original starter?`);
	if (!ok) return;
	copyFileSync(fixture, starter);
	ctx.ui.notify("Starter reset", "info");
}

function sendExplainPrompt(pi: ExtensionAPI): void {
	pi.sendUserMessage(
		[
			"/skill:allium",
			"We are in Allium Tutor lesson 01.",
			"Please explain `library-starter.allium` slowly, focusing only on the TODO for `BorrowBook`.",
			"Do not edit files yet. Teach one idea at a time.",
		].join("\n"),
	);
}

function sendSmallestEditPrompt(pi: ExtensionAPI): void {
	pi.sendUserMessage(
		[
			"/skill:allium",
			"We are in Allium Tutor lesson 01.",
			"Make the smallest safe edit to `library-starter.allium`: add only the missing `BorrowBook` rule at the TODO.",
			"Do not read the solution file. After editing, run `allium check library-starter.allium` and `allium analyse library-starter.allium`.",
		].join("\n"),
	);
}

export default function alliumTutor(pi: ExtensionAPI) {
	pi.on("session_start", (_event, ctx) => {
		pi.setSessionName("Allium Tutor: Lesson 01");
		setTutorWidget(ctx);
	});

	pi.registerCommand("allium-tutor", {
		description: "Open the guided Allium lesson menu",
		getArgumentCompletions: (prefix) => {
			const items = ["lesson-01", "status"];
			return items.filter((item) => item.startsWith(prefix)).map((item) => ({ value: item, label: item }));
		},
		handler: async (_args, ctx) => {
			setTutorWidget(ctx);

			if (ctx.mode !== "tui") {
				ctx.ui.notify("Allium Tutor needs Pi interactive TUI mode", "warning");
				return;
			}

			while (true) {
				const choice = await ctx.ui.select("Allium Tutor — choose one small step", ACTIONS);
				if (!choice || choice === "Exit") return;

				if (choice.startsWith("1.")) {
					await showLessonCard(ctx);
				} else if (choice.startsWith("2.")) {
					await runAllium(pi, ctx);
				} else if (choice.startsWith("3.")) {
					sendExplainPrompt(pi);
					return;
				} else if (choice.startsWith("4.")) {
					await editStarter(ctx);
				} else if (choice.startsWith("5.")) {
					await runAllium(pi, ctx);
				} else if (choice.startsWith("6.")) {
					await showHint(ctx);
				} else if (choice.startsWith("7.")) {
					sendSmallestEditPrompt(pi);
					return;
				} else if (choice.startsWith("8.")) {
					await showAnswer(ctx);
				} else if (choice.startsWith("9.")) {
					await resetStarter(ctx);
				}
			}
		},
	});
}
