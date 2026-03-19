# Tasks: Implement iOS MeetingListView

## 1. Implementation
- [x] 1.1 Create MeetingListView with Liquid Glass design
- [x] 1.2 Integrate with existing MeetingListViewModel
- [x] 1.3 Implement meeting grouping by status (SCHEDULED, STARTED, ENDED, CANCELLED)
- [x] 1.4 Add pull-to-refresh functionality
- [x] 1.5 Create empty state view
- [x] 1.6 Create loading state view
- [x] 1.7 Implement MeetingCard component
  - [x] Platform icon with color
  - [x] Title and description
  - [x] Date and duration display
  - [x] Status badge
  - [x] Meeting link with copy button
  - [x] Organizer actions (Edit, Regenerate Link, Share)
- [x] 1.8 Create MeetingEditSheet
  - [x] Title and description fields
  - [x] Date/time picker integration
  - [x] Duration picker (hours/minutes)
  - [x] Platform selection grid
  - [x] Save/Cancel buttons
- [x] 1.9 Create MeetingGenerateLinkSheet
  - [x] Platform selection grid
  - [x] Visual selection indicators
  - [x] Info box for link replacement
  - [x] Generate/Cancel buttons

## 2. Testing (TODO - @tests)
- [ ] 2.1 Create MeetingListViewModel tests
  - [ ] Test loadMeetings()
  - [ ] Test createMeeting()
  - [ ] Test updateMeeting()
  - [ ] Test cancelMeeting()
  - [ ] Test generateMeetingLink()
  - [ ] Test error handling
- [ ] 2.2 Create MeetingListView UI tests
  - [ ] Test pull-to-refresh
  - [ ] Test empty state display
  - [ ] Test loading state display
  - [ ] Test status grouping
  - [ ] Test meeting card tap
  - [ ] Test sheet presentation
- [ ] 2.3 Create MeetingEditSheet tests
  - [ ] Test field validation
  - [ ] Test date/time picker
  - [ ] Test duration picker
  - [ ] Test platform selection
  - [ ] Test save action
- [ ] 2.4 Create MeetingGenerateLinkSheet tests
  - [ ] Test platform selection
  - [ ] Test generate action
- [ ] 2.5 Create accessibility tests
  - [ ] Test VoiceOver labels
  - [ ] Test trait correctness
  - [ ] Test hint accuracy

## 3. Review (TODO - @review)
- [ ] 3.1 Review design system compliance
  - [ ] Liquid Glass component usage
  - [ ] Color scheme consistency
  - [ ] Spacing and layout
  - [ ] Typography hierarchy
- [ ] 3.2 Review accessibility
  - [ ] VoiceOver labels
  - [ ] Color contrast ratios
  - [ ] Touch target sizes (minimum 44x44pt)
- [ ] 3.3 Review UX flow
  - [ ] Meeting card interaction
  - [ ] Sheet presentation
  - [ ] Status grouping logic
  - [ ] Platform selection feedback

## 4. Documentation
- [x] 4.1 Update context.md with implementation details
- [x] 4.2 Update context-log.jsonl with decisions and artifacts
- [ ] 4.3 Update user documentation if needed

## 5. Integration
- [ ] 5.1 Test with actual backend data
- [ ] 5.2 Test offline scenarios
- [ ] 5.3 Test navigation flows
- [ ] 5.4 Verify compilation on Xcode
