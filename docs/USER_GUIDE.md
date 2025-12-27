# Wakeve - User Guide

## Table of Contents

1. [Getting Started](#getting-started)
2. [Event Organization](#event-organization)
3. [Voting & Scenarios](#voting--scenarios)
4. [Budget Planning](#budget-planning)
5. [Logistics Planning](#logistics-planning)
6. [Collaboration](#collaboration)
7. [Suggestions](#suggestions)
8. [Meetings](#meetings)
9. [Payments](#payments)

---

## Getting Started

### Installation

#### Android
1. Download the APK from the latest release
2. Install and open the app
3. Sign in with Google or Apple ID

#### iOS
1. Download from App Store
2. Install and open the app
3. Sign in with Apple ID or Google

### First Steps

1. **Create your profile**
   - Add your name and photo
   - Set your preferences (budget, activities, seasons)

2. **Create your first event**
   - Tap the "+" button
   - Enter event title and description
   - Invite friends

3. **Start planning!**
   - Propose dates for voting
   - Create scenarios for comparison
   - Add budget items
   - Plan logistics

---

## Event Organization

### Creating an Event

1. Tap the **"+"** button on the home screen
2. Fill in event details:
   - **Title**: Event name (required)
   - **Description**: Event details (optional)
   - **Deadline**: Voting deadline date (required)
   - **Duration**: Estimated duration (optional)

3. Add participants:
   - Search by email or name
   - Or invite via link

4. Set initial event status:
   - **DRAFT**: Event is being planned
   - **POLLING**: Open for date voting

### Managing an Event

From the event detail screen you can:
- **Edit event**: Update title, description, dates
- **Invite participants**: Add more people
- **Delete event**: Remove event (if you're organizer)

### Event Status Workflow

```
DRAFT → POLLING → COMPARING → CONFIRMED → ORGANIZING → FINALIZED
```

- **DRAFT**: Initial planning phase
- **POLLING**: Voting on dates/scenarios
- **COMPARING**: Comparing different scenarios
- **CONFIRMED**: Date finalized
- **ORGANIZING**: Detailed planning in progress
- **FINALIZED**: All details confirmed
- **CANCELLED**: Event cancelled

---

## Voting & Scenarios

### Creating Polls

1. From event details, tap **"Propose Dates"**
2. Add date proposals:
   - **Start date**: When the event begins
   - **End date**: When it ends
   - **Timezone**: Select appropriate timezone
3. Set voting rules:
   - Allow participants to add dates (optional)
   - Enable weight voting (YES=2, MAYBE=1, NO=-1)

### Voting on Dates

1. Open the event
2. Tap on the **Poll** section
3. For each date option, select:
   - **✅ YES**: You can definitely make it
   - **🤔 MAYBE**: You might make it
   - **❌ NO**: You cannot make it
4. Tap **"Submit Votes"**

### Creating Scenarios

Scenarios allow comparing different event options:

1. Tap **"Create Scenario"**
2. Fill in scenario details:
   - **When**: Date or date range
   - **Where**: Destination
   - **Duration**: Number of days/nights
   - **Group**: Expected participants
   - **Budget**: Estimated cost per person

3. View all scenarios in a comparison table

### Voting on Scenarios

1. Open **Scenarios** tab
2. For each scenario, select:
   - **PREFER**: Your top choice
   - **NEUTRAL**: No preference
   - **AGAINST**: You don't like this option

The system calculates the best scenario based on:
- PREFER = 2 points
- NEUTRAL = 1 point
- AGAINST = -1 point

### Confirming a Scenario

Once voting is complete, the organizer can:
1. View voting results
2. Select the **best scenario** (★ indicates winner)
3. Tap **"Confirm Scenario"**
4. Event status changes to **CONFIRMED**

---

## Budget Planning

### Creating a Budget

1. From event details (status: ORGANIZING), tap **"Budget"**
2. Tap **"Create Budget"**
3. Set budget parameters:
   - **Target amount**: Total budget goal
   - **Currency**: EUR, USD, etc.

### Adding Budget Items

Budget items track event expenses:

1. Tap **"+"** in budget screen
2. Fill in item details:
   - **Category**: Accommodation, Transport, Meals, Activities, Equipment, Other
   - **Name**: Item description
   - **Estimated**: Expected cost
   - **Actual**: Real cost (updated after purchase)
   - **Paid by**: Who paid

3. Mark item as **Paid** when purchased

### Budget Categories

- **Accommodation**: Hotels, Airbnbs, etc.
- **Transport**: Flights, trains, car rentals
- **Meals**: Restaurant, groceries
- **Activities**: Tickets, excursions
- **Equipment**: Gear, rentals
- **Other**: Miscellaneous expenses

### Budget Overview

The overview screen shows:
- **Total budget**: Target amount
- **Total spent**: Sum of actual costs
- **Remaining**: Budget - Spent
- **By category**: Breakdown by category
- **Per person**: Cost divided by participants

### Settlements

The system automatically calculates who owes whom based on:
- Who paid for what
- How much each participant owes or is owed

View settlements in the **"Settlements"** section:
- Shows suggested payments
- Mark as completed when paid

---

## Logistics Planning

### Accommodation

#### Adding Accommodation

1. Tap **"Add Accommodation"**
2. Fill in details:
   - **Name**: Hotel/rental name
   - **Type**: Hotel, Airbnb, Camping, etc.
   - **Address**: Location
   - **Price/night**: Cost per night
   - **Max capacity**: How many people it holds

3. Set booking status:
   - **Searching**: Looking for options
   - **Reserved**: Hold reserved
   - **Confirmed**: Booking confirmed
   - **Cancelled**: Booking cancelled

#### Assigning Participants to Rooms

1. From accommodation details, tap **"Assign Rooms"**
2. For each room, select participants
3. System suggests optimal assignments
4. You can manually override

### Meals

#### Auto-Generating Meals

1. Tap **"Meals"**
2. Tap **"Auto-generate"**
3. Configure:
   - **Date range**: Start and end dates
   - **Meal types**: Breakfast, Lunch, Dinner, Snacks
   - **Cost/meal**: Estimated cost
4. Tap **"Generate"**

#### Managing Dietary Restrictions

1. Tap **"Dietary Restrictions"**
2. Add for each participant:
   - Vegetarian
   - Vegan
   - Gluten-free
   - Dairy-free
   - Nut allergies
   - Shellfish allergies
   - Other

Generated meals will account for these restrictions.

### Equipment

#### Auto-Generating Checklist

1. Tap **"Equipment"**
2. Tap **"Generate Checklist"**
3. Select event type:
   - **Camping**: Tents, sleeping bags, etc.
   - **Beach**: Sunscreen, towels, etc.
   - **Ski**: Skis, helmets, etc.
   - **Hiking**: Backpacks, boots, etc.
   - **Picnic**: Blankets, utensils, etc.

#### Managing Equipment Status

For each item, track:
- **NEEDED**: Item required
- **ASSIGNED**: Someone to bring it
- **CONFIRMED**: Confirmed they'll bring it
- **PACKED**: Item is packed

### Activities

#### Adding Activities

1. Tap **"Activities"**
2. Fill in details:
   - **Title**: Activity name
   - **Date & Time**: When it happens
   - **Location**: Where it is
   - **Duration**: How long it lasts
   - **Cost per person**: Price
   - **Max participants**: Capacity limit

#### Managing Registrations

1. Open activity details
2. Tap **"Participants"**
3. See who registered
4. Tap to remove someone if needed

---

## Collaboration

### Comments

#### Adding Comments

1. From any section (Event, Scenario, Budget, Accommodation, Meals, Equipment, Activities)
2. Tap the **comment icon** (💬)
3. Type your message
4. Tap **"Send"**

Comment sections:
- **General**: Event-wide discussions
- **Scenario**: Specific scenario discussions
- **Budget**: Expense discussions
- **Transport**: Travel coordination
- **Accommodation**: Lodging discussions
- **Meals**: Food planning
- **Equipment**: Gear coordination
- **Activities**: Activity discussions

#### Replying to Comments

1. Tap on a comment
2. Tap **"Reply"**
3. Type your response
4. Tap **"Send"**

#### Managing Comments

- **Edit**: Modify your own comments (long-press)
- **Delete**: Remove your own comments
- **Filter**: Show comments for specific section

### Notifications

You'll receive notifications for:
- **New comments**: When someone comments
- **Replies**: When someone replies to you
- **Event updates**: Status changes, confirmations

---

## Suggestions

### Personalizing Your Preferences

1. Tap your profile picture
2. Go to **"Preferences"**
3. Set your preferences:
   - **Budget range**: Min and max per event
   - **Duration**: Preferred event length (days)
   - **Seasons**: Best seasons for you
   - **Activities**: Things you like
   - **Locations**: Preferred regions

### Getting Suggestions

Based on your preferences, you'll receive suggestions for:
- **Scenarios**: Recommended dates/destinations
- **Activities**: Things to do
- **Restaurants**: Places to eat
- **Destinations**: Where to go
- **Accommodation**: Where to stay

Suggestions are scored (0-100):
- **Cost**: Within budget? (30%)
- **Personalization**: Matches preferences? (25%)
- **Accessibility**: Easy to get to? (20%)
- **Seasonality**: Good time to go? (15%)
- **Popularity**: Do others like it? (10%)

### Using Suggestions

1. Review suggested items
2. See reasons why recommended (e.g., "In your budget", "Good season")
3. Tap **"Use This"** to apply suggestion
4. Or tap **"Dismiss"** if not interested

---

## Meetings

### Creating Virtual Meetings

For remote coordination:

1. From event details, tap **"Create Meeting"**
2. Choose platform:
   - **Zoom**: Zoom meeting with password
   - **Google Meet**: Google Meet link
   - **FaceTime**: Group FaceTime (iOS only)
3. Fill in details:
   - **Title**: Meeting name
   - **Date & Time**: When to meet
   - **Duration**: Meeting length
   - **Timezone**: Correct timezone

4. Configure options:
   - **Require password**: Add meeting password
   - **Waiting room**: Participants wait before admitted
   - **Participant limit**: Max attendees

### Meeting Features

#### Zoom
- **Meeting ID**: 10-digit number
- **Password**: Optional (6 characters)
- **Waiting room**: Host admits participants
- **Host key**: Control meeting

#### Google Meet
- **Meeting code**: 10 characters (e.g., abc-defgh)
- **No password**: Open by default
- **Easy to join**: No app needed for viewers

#### FaceTime
- **Apple ID**: Uses organizer's ID
- **Group FaceTime**: All participants need Apple IDs
- **iOS only**: Not available on Android

### Invitations

1. After creating meeting, tap **"Send Invitations"**
2. Invitations sent to validated participants
3. Participants can:
   - **Accept**: They'll join
   - **Decline**: They won't join
   - **Tentative**: Maybe

### Reminders

Automatic reminders sent:
- **1 day before**: "Meeting tomorrow at [time]"
- **1 hour before**: "Meeting in 1 hour"
- **15 min before**: "Meeting in 15 minutes"
- **5 min before**: "Meeting starting soon"

---

## Payments

### Creating a Money Pot

For group contributions:

1. Tap **"Create Pot"**
2. Set pot details:
   - **Title**: What the pot is for
   - **Description**: Optional details
   - **Target amount**: Goal amount
   - **Currency**: EUR, USD, etc.
   - **Expires**: Deadline for contributions
   - **Public**: Visible to all or organizer only

### Contributing to a Pot

1. Open event details
2. Tap on pot
3. Tap **"Contribute"**
4. Enter amount:
   - **Amount**: How much to contribute
   - **Payment method**: Card, PayPal, etc.
   - **Anonymous**: Show or hide your name
5. Tap **"Pay"**

### Recording Expenses

Track shared expenses:

1. From event, tap **"Add Expense"**
2. Fill in expense:
   - **Paid by**: Who paid
   - **Category**: Type of expense
   - **Amount**: How much
   - **Date**: When purchased
3. Choose **split type**:
   - **Equal**: Split evenly among all
   - **Percentage**: Custom percentages
   - **Custom**: Complex splits

### Settlements

See who owes whom:

1. Tap **"Settlements"**
2. View suggested payments
3. Tap **"Mark Paid"** when settled
4. Settled items disappear from list

### Tricount Integration

Coming soon:
- **Sync expenses**: Automatically sync to Tricount
- **Share link**: Send Tricount link to participants
- **View in Tricount**: Open in web app

---

## Tips & Best Practices

### Planning an Event

1. **Start early**: Create event weeks/months in advance
2. **Propose multiple dates**: Give options for voting
3. **Use scenarios**: Compare different options clearly
4. **Set deadlines**: Give people time to vote

### Budgeting

1. **Track everything**: Add expenses as they happen
2. **Be realistic**: Budget for unexpected costs
3. **Update actual costs**: Don't forget to update after purchases

### Collaboration

1. **Communicate early**: Use comments for discussions
2. **Be respectful**: Everyone has different preferences
3. **Stay organized**: Use sections for different topics

### Offline Mode

Wakeve works offline! Changes sync when you're back online.

---

## FAQ

### Q: Can I use Wakeve without internet?

A: Yes! All core features work offline. Changes sync automatically when you reconnect.

### Q: How do I invite people?

A: Share the event link, or search for their email/phone number.

### Q: What happens if people don't vote?

A: The organizer can manually select the best scenario after the deadline.

### Q: Can I change my vote?

A: Yes! You can update your vote until the deadline passes.

### Q: How is the budget calculated per person?

A: Total expenses divided by number of participants, minus each person's contributions.

### Q: Is my payment info secure?

A: Yes! We use industry-standard encryption and don't store card details.

---

## Support

Having trouble?

- **Email**: support@wakeve.app
- **Twitter**: @wakeve_app
- **Documentation**: https://docs.wakeve.app

---

**Version**: 1.0.0  
**Last Updated**: December 26, 2025
