# Hint: Temporal overdue

The `overdue` status is currently unreachable. You need one rule that observes time instead of a member action.

Use a temporal trigger:

```allium
when: _: Book.due_at <= now
```

Then guard it so it only fires while the book is still borrowed:

```allium
requires: book.status = borrowed
```

The rule should set `book.status = overdue` and emit `BookOverdue(...)`.
