# Hint — Reachable trigger

The rule listens for this event:

```allium
when: MemberCancelsHold(member)
```

But Allium also needs to know where that event can enter the system.

Add it under the surface's `provides:` block:

```allium
provides:
    MemberCancelsHold(member)
        when request.active
```

Rule triggers usually come from either:

- a `surface provides:` entry, or
- another rule emitting a trigger in `ensures:`.
