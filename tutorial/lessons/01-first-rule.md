# Lesson 01 — First Rule: Borrowing a Book

You will move slowly from one English behavior to one checked Allium rule.

## Idea

English behavior:

> When a member borrows an available book, the book becomes borrowed, records the borrower, receives a due date, and emits a `BookBorrowed` fact.

Allium captures that behavior as a rule:

```allium
rule BorrowBook {
    when: MemberBorrowsBook(member)
    requires: book.status = on_shelf
    ensures: book.status = borrowed
    ensures: book.borrowed_by = member.name
    ensures: book.due_at = now + config.loan_period
    ensures: BookBorrowed(book: book, member: member)
}
```

## What to notice

- `when` says what event wakes the rule up.
- `requires` says when this case applies.
- `ensures` says what observable behavior must result.
- The rule does not say anything about databases, endpoints, buttons, classes, or shell commands.

## Your task

Open `library-starter.allium` in the lesson workspace and add the missing `BorrowBook` rule where the TODO appears.

Then run:

```bash
allium check library-starter.allium
allium analyse library-starter.allium
```

Before your edit, `allium check` should complain that `borrowed` is never assigned. After your edit, both commands should be clean.

## If you get stuck

Ask Pi:

```text
/skill:allium Explain the TODO in library-starter.allium slowly. Do not edit yet.
```

Then ask for the smallest edit only after you understand the rule shape.
