# Hint — First rule

The checker says `borrowed` is never assigned because no rule currently moves the book into the borrowed state.

Add this rule where the TODO appears:

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

Read it as: event → guard → state change → attribution → due date → emitted fact.
