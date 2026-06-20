# Change: Add contact participant selection

## Why
Organizers currently add participants one email at a time, even though the most common source of invitees is the device address book. This slows down event setup and increases typing errors.

## What Changes
- Add a contacts entry point to the existing participant management screen.
- Let organizers select contacts with email addresses and add the selected emails as pending participants.
- Keep contact access local to the device and persist only emails explicitly selected by the organizer.
- Preserve manual email entry and the existing invitation/participant backend contract.

## Impact
- Affected specs: `event-organization`, `collaboration-management`
- Affected code: shared participant contact policy, Android participant management UI, iOS participant management UI, platform permission strings
