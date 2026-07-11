import { isAbsolute, resolve, relative, sep } from "node:path";
import type { ExtensionAPI } from "@earendil-works/pi-coding-agent";
import { createLocalBashOperations } from "@earendil-works/pi-coding-agent";

type ToolInput = Record<string, unknown>;

function lessonRoot(cwd: string): string {
	return resolve(process.env.ALLIUM_TUTOR_WORKSPACE || process.env.LESSON_ROOT || cwd);
}

function stripAt(path: string): string {
	return path.startsWith("@") ? path.slice(1) : path;
}

function isInside(path: string, root: string): boolean {
	const rel = relative(root, path);
	return rel === "" || (rel !== ".." && !rel.startsWith(`..${sep}`) && !isAbsolute(rel));
}

function relativeSegments(path: string, root: string): string[] {
	const rel = relative(root, path);
	return rel.split(sep).filter(Boolean);
}

function isSensitivePath(path: string, root: string): boolean {
	const segments = relativeSegments(path, root);
	const base = segments[segments.length - 1] || "";
	return (
		segments.includes(".git") ||
		segments.includes(".ssh") ||
		segments.includes(".aws") ||
		segments.includes(".gnupg") ||
		base === ".env" ||
		base.startsWith(".env.") ||
		base.endsWith(".pem") ||
		base.endsWith(".key") ||
		base.toLowerCase().includes("token") ||
		base.toLowerCase().includes("secret")
	);
}

function pathVerdict(rawPath: unknown, root: string, writeLike: boolean): { ok: true; path: string } | { ok: false; reason: string } {
	if (typeof rawPath !== "string" || rawPath.trim() === "") {
		return { ok: false, reason: "tool path is missing or not a string" };
	}

	const cleaned = stripAt(rawPath.trim());
	const absolute = resolve(root, cleaned);

	if (!isInside(absolute, root)) {
		return { ok: false, reason: `path is outside the lesson workspace: ${rawPath}` };
	}

	if (writeLike && isSensitivePath(absolute, root)) {
		return { ok: false, reason: `writes to sensitive lesson paths are blocked: ${rawPath}` };
	}

	return { ok: true, path: absolute };
}

function hasShellMetacharacters(command: string): boolean {
	return /[;&|`$<>\\\n\r]/.test(command);
}

function tokenizesSafely(command: string): string[] | undefined {
	if (command.trim() !== command || command.length === 0) return undefined;
	if (hasShellMetacharacters(command)) return undefined;
	if (command.includes("\"") || command.includes("'")) return undefined;
	return command.split(/\s+/).filter(Boolean);
}

function isSafeTarget(token: string, root: string): boolean {
	if (token === "" || token.startsWith("-")) return false;
	const absolute = resolve(root, token);
	return isInside(absolute, root) && !relativeSegments(absolute, root).includes("..");
}

function validateBashCommand(command: string, root: string): { ok: true } | { ok: false; reason: string } {
	const tokens = tokenizesSafely(command);
	if (!tokens) {
		return { ok: false, reason: "only simple lesson commands are allowed; shell metacharacters, quotes, pipes, redirects, and command substitution are blocked" };
	}

	const [program, subcommand, ...rest] = tokens;

	if (program === "pwd" && tokens.length === 1) return { ok: true };

	if (program === "ls") {
		const args = tokens.slice(1);
		for (const arg of args) {
			if (arg.startsWith("-")) {
				if (!/^-?[A-Za-z]+$/.test(arg)) return { ok: false, reason: `unsupported ls option: ${arg}` };
				continue;
			}
			if (!isSafeTarget(arg, root)) return { ok: false, reason: `ls target is outside the lesson workspace: ${arg}` };
		}
		return { ok: true };
	}

	if (program === "allium" && subcommand === "--version" && tokens.length === 2) return { ok: true };

	if (program === "allium" && ["check", "analyse", "plan"].includes(subcommand || "")) {
		if (rest.length === 0) return { ok: false, reason: `allium ${subcommand} needs a lesson file or directory target` };
		for (const arg of rest) {
			if (!isSafeTarget(arg, root)) return { ok: false, reason: `allium target is outside the lesson workspace: ${arg}` };
		}
		return { ok: true };
	}

	if (program === "git" && subcommand === "diff") {
		const args = rest;
		if (args.length === 0) return { ok: true };
		if (args[0] !== "--") return { ok: false, reason: "only `git diff` or `git diff -- <lesson-file>` is allowed" };
		for (const arg of args.slice(1)) {
			if (!isSafeTarget(arg, root)) return { ok: false, reason: `git diff target is outside the lesson workspace: ${arg}` };
		}
		return { ok: true };
	}

	return {
		ok: false,
		reason: `command is outside the lesson allowlist: ${command}`,
	};
}

function shQuote(value: string): string {
	return `'${value.replace(/'/g, `'\\''`)}'`;
}

function wrapBashCommand(command: string, root: string): string {
	const home = process.env.ALLIUM_TUTOR_HOME || resolve(root, "../home");
	const tmp = process.env.ALLIUM_TUTOR_TMP || resolve(root, "../tmp");
	const path = process.env.PATH || "/usr/bin:/bin:/usr/sbin:/sbin";
	return [
		`export HOME=${shQuote(home)}`,
		`export TMPDIR=${shQuote(tmp)}`,
		`export PATH=${shQuote(path)}`,
		`cd ${shQuote(root)}`,
		command,
	].join(" && ");
}

export default function isolationGuard(pi: ExtensionAPI) {
	pi.on("session_start", (_event, ctx) => {
		const root = lessonRoot(ctx.cwd);
		ctx.ui.setStatus("allium-tutor-guard", ctx.ui.theme.fg("accent", `lesson root: ${root}`));
	});

	pi.on("tool_call", async (event, ctx) => {
		const root = lessonRoot(ctx.cwd);
		const input = event.input as ToolInput;

		if (event.toolName === "read" || event.toolName === "write" || event.toolName === "edit") {
			const writeLike = event.toolName === "write" || event.toolName === "edit";
			const verdict = pathVerdict(input.path, root, writeLike);
			if (!verdict.ok) {
				ctx.ui.notify(`Blocked ${event.toolName}: ${verdict.reason}`, "warning");
				return { block: true, reason: verdict.reason };
			}
		}

		if (event.toolName === "bash") {
			const command = input.command;
			if (typeof command !== "string") {
				return { block: true, reason: "bash command is missing or not a string" };
			}

			const verdict = validateBashCommand(command, root);
			if (!verdict.ok) {
				ctx.ui.notify(`Blocked bash: ${verdict.reason}`, "warning");
				return { block: true, reason: verdict.reason };
			}

			input.command = wrapBashCommand(command, root);
		}

		return undefined;
	});

	pi.on("user_bash", (event, ctx) => {
		const root = lessonRoot(ctx.cwd);
		const verdict = validateBashCommand(event.command, root);
		if (!verdict.ok) {
			return {
				result: {
					output: `Blocked by Allium Tutor isolation guard: ${verdict.reason}\n`,
					exitCode: 1,
					cancelled: false,
					truncated: false,
				},
			};
		}

		const local = createLocalBashOperations();
		return {
			operations: {
				exec(command, _cwd, options) {
					return local.exec(wrapBashCommand(command, root), root, options);
				},
			},
		};
	});
}
