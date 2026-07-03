# Whisper Project Guide (Sovereign Communication)

## 🌌 Project Vision
Whisper is a decentralized social application built on the Nostr protocol. The goal is to create a "Sovereign" communication tool that prioritizes deep connection over dopamine-driven attention. 
**Core Philosophy:** Eliminate "Attention Traps" (no red notification dots, no addictive loops, no ego-boosters). Move from "Attention" to "Intention".

## 🛠 Technical Stack
- **Platform:** Android Native
- **UI Framework:** Jetpack Compose (Declarative UI)
- **Language:** Kotlin
- **Architecture:** MVVM (Model-View-ViewModel)
- **Async/Streams:** Kotlin Coroutines & Flow
- **Protocol:** Nostr (via `nostr-tools` or equivalent Kotlin library)
- **Connectivity:** WebSockets for real-time event streams

## 🎨 UX & Design Principles (The Sovereign Aesthetic)
- **Ambient Awareness:** No aggressive push notifications. Use "Ambient Indicators" instead.
- **The Blue Glow:** 
    - Use a soft, light-blue glow for event indicators (follows/replies).
    - Transition: Slow fade-in (no abrupt blinking).
    - Interaction: Clicking the glow opens a lightweight overlay, not a full new page.
- **Anti-Casino:** 
    - No notification counters (e.g., "99+").
    - No sorting by "most popular" (avoid popularity contests).
    - Focus on "Resonance" and "Intentionality".

## ⚙️ Technical Implementation Details
- **Event Handling:** 
    - Focus on Kind 0 (Follows) and Kind 1 (Replies/Posts).
    - Use WebSocket listeners to monitor the stream of events.
    - Filter events by the user's own pubkey to identify interactions.
- **State Management:** 
    - UI state should be driven by Kotlin Flows from the ViewModel.
    - The "Blue Glow" state should be a boolean/enum handled in the ViewModel and observed by the Compose UI.

## 📜 Guiding Rules for Claude
1. **Modular Code:** Write small, testable functions. Do not generate massive files.
2. **Modern Android:** Always use Jetpack Compose and modern Kotlin idioms. No XML layouts.
3. **Context Preservation:** Always check the current `CLAUDE.md` before suggesting architectural changes.
4. **Sovereign Mindset:** If a suggested feature feels like a "social media trap" (e.g., a like-counter), flag it and suggest a more intentional alternative.

## 🚀 Current Milestones
- [ ] Basic Android Scaffold with MVVM.
- [ ] Nostr Relay connection and basic event publishing.
- [ ] Implementation of the WebSocket listener for Kind 0/1 events.
- [ ] Development of the "Blue Glow" ambient indicator and overlay.

#Pages
We will have a public and a private feed.

In the public feed all posts with 1 or more replies is shown. This is for engagement recognition. The user finds users who resonate in that feed and decides to follow. Then the user is moved to the private feed there the user of the app will find all posts of users he follows.

We use Nostr Compass (https://nostrcompass.org/en/topics/quartz/) for the connection to nostr.

When the user first uses the app he is asked to input his nsec. 

The background is white. The text is black. Follow buttons are Black with White text, and unfollow buttons are bordered with black, inside it's white. and text is black.

