## 1. Specification
- [x] 1.1 Add OpenSpec deltas for contact-based participant selection.
- [x] 1.2 Validate the OpenSpec change with `openspec validate --strict`.

## 2. Shared Logic
- [x] 2.1 Add shared contact candidate and selection result models.
- [x] 2.2 Add email normalization, filtering, and duplicate handling for contact-selected participants.
- [x] 2.3 Add focused common tests for the contact selection policy.

## 3. Android
- [x] 3.1 Declare contacts permission and add a ContactsContract loader.
- [x] 3.2 Add a contacts button and multi-select picker to participant management.
- [x] 3.3 Wire selected contact emails through the existing repository addParticipant flow.

## 4. iOS
- [x] 4.1 Add contacts usage description.
- [x] 4.2 Add Contacts.framework loading with permission handling.
- [x] 4.3 Add a search and multi-select contact sheet to ParticipantManagementView.

## 5. Verification
- [x] 5.1 Run focused shared tests.
- [x] 5.2 Run relevant Android/iOS compile or available tests.
