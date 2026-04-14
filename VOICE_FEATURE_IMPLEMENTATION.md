# Voice Feature Implementation Guide - PitchPerfect

## Overview
This document explains how the voice-to-text, AI analysis, and text-to-speech features work in PitchPerfect. Follow this guide to implement or extend the voice conversation system.

---

## Architecture

### 1. **Conversation Flow**
```
User Clicks Record 
    ↓
AI Welcome Message (TTS)
    ↓
User speaks → Speech Recognition (STT)
    ↓
Text is collected in real-time
    ↓
Silence Detection (1.8 seconds)
    ↓
AI Analysis & Smart Response
    ↓
AI Feedback (TTS)
    ↓
Session ends (user clicks Record again to start new session)
```

---

## Components

### A. **Speech Recognition (STT) - SpeechRecognizer**
**Purpose:** Convert user's voice to text in real-time

**Key Implementation:**
- Uses Android's built-in `SpeechRecognizer` class
- Listens continuously while recording
- Handles partial and final results
- Located in: `PracticeActivity.setupSpeech()`

**Key Methods:**
```java
startListening()           // Start listening for speech
speechRecognizer.setRecognitionListener() // Handle speech events
onResults()                // Final speech result
onPartialResults()         // Real-time partial text
```

**Features:**
- Real-time transcript display
- Automatic restart after each sentence
- Error handling with auto-recovery

---

### B. **Silence Detection**
**Purpose:** Detect when user stops talking to trigger AI feedback

**How it works:**
- Timer set to `1800ms` (1.8 seconds)
- Resets when user says something new
- Triggers `handleConversationalAI()` when silence timeout occurs

**Code location:** `resetSilenceTimer()` and `stopSilenceTimer()`

**Customization:**
```java
private static final long SILENCE_THRESHOLD = 1800; // milliseconds
// Lower = faster response, Higher = allows longer pauses
```

---

### C. **Intelligent AI Analysis - handleConversationalAI()**
**Purpose:** Analyze user's speech and generate contextual responses

**How it works:**
1. Checks user's text for keywords
2. Matches keywords to pitch topics (problem, solution, team, market, etc.)
3. Generates appropriate follow-up question

**Keyword Categories:**
- **Problem/Pain:** "hello", "hi", "hey" → Ask about the problem
- **Solution Focus:** "solution", "product", "app" → Ask about uniqueness
- **Team Building:** "team", "founder", "experience" → Ask about team strength
- **Revenue Model:** "money", "revenue", "business" → Ask about profitability
- **Market Validation:** "market", "user", "customer" → Ask about demand
- **Growth Plans:** "growth", "scale", "next" → Ask about obstacles
- **Competition:** "competitor", "rival" → Ask about differentiation

**Fallback Responses:** If no keywords match, uses contextual generic replies

**Location:** `handleConversationalAI()` method (lines 210-260)

---

### D. **Text-to-Speech (TTS) - TextToSpeech**
**Purpose:** AI speaks responses aloud

**Key Properties:**
- Language: US English (`Locale.US`)
- Utterance IDs track individual speeches
- Progress listener detects when AI finishes speaking

**Methods:**
```java
speakReply(String text, String id)     // Queue and speak text
```

**States:**
- `isAIspeaking = true` → User listening to AI (STT paused)
- `isAIspeaking = false` → AI done, ready to listen again

**Important:** Prevents user and AI from talking simultaneously

---

### E. **Final AI Feedback - stopRecordingAndSubmit()**
**Purpose:** Give comprehensive analysis when user stops recording

**Current Final Feedback Types:**
1. **If transcript empty:** Encourage user to speak
2. **If mentions "team":** Ask about team cohesion
3. **If mentions "market":** Ask about competitive defense
4. **If mentions "money":** Ask about revenue milestones
5. **If long transcript:** Ask about "killer feature"
6. **Default:** Ask to dive deeper into technology

**Location:** `stopRecordingAndSubmit()` method (lines 310-380)

---

## Session Flow with Code Execution

### Step 1: User Clicks Record Button
```java
binding.btnRecord.setOnClickListener(v -> {
    if (!isRecording) startIntroFlow();
});
```

### Step 2: AI Gives Intro (TTS)
```java
private void startIntroFlow() {
    speakReply("Hello! This is Pitch Perfect. I'm ready to listen to your amazing pitch. Whenever you're ready, start speaking!", "intro");
}
```
→ Triggers `onDone("intro")` in TTS listener, which calls `startActualRecording()`

### Step 3: Start Recording & Listening
```java
private void startActualRecording() {
    // MediaRecorder captures audio file
    // Speech recognition starts
    startListening();
}
```

### Step 4: Real-time Transcript Updates
```java
onResults() {
    fullTranscript.append(text);
    resetSilenceTimer(text);  // Start waiting for silence
    startListening();          // Keep listening
}
```

### Step 5: Silence Detected → AI Feedback
```java
resetSilenceTimer() → [1.8 sec silence] → handleConversationalAI() → speakReply()
```

### Step 6: Session Ends
- User clicks "Stop Recording" button
- Final analysis provided
- Session data submitted
- Ready for next recording

---

## Key Variables to Understand

| Variable | Type | Purpose |
|----------|------|---------|
| `fullTranscript` | StringBuilder | Accumulates all recognized speech |
| `currentPartialSpeech` | String | Current partial recognition |
| `isRecording` | boolean | Is active session in progress |
| `isAIspeaking` | boolean | AI currently speaking (STT paused) |
| `isTtsReady` | boolean | TextToSpeech engine ready |
| `silenceRunnable` | Runnable | Silence detection timer |

---

## Customization Guide

### Change Welcome Message
**File:** `PracticeActivity.java`
**Method:** `startIntroFlow()`
```java
speakReply("Your custom welcome message here!", "intro");
```

### Change Feedback Keywords
**File:** `PracticeActivity.java`
**Method:** `handleConversationalAI()`
```java
if (text.contains("your_keyword")) {
    reply = "Your custom response here";
}
```

### Change Silence Timeout
**File:** `PracticeActivity.java`
**Line:** Around `SILENCE_THRESHOLD`
```java
private static final long SILENCE_THRESHOLD = 2500; // 2.5 seconds
```

### Change Final Feedback
**File:** `PracticeActivity.java`
**Method:** `stopRecordingAndSubmit()`
- Modify the feedback strings based on transcript analysis

---

## Permissions Required

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
```

Both are already in `AndroidManifest.xml`

---

## Testing Checklist

- [ ] App requests microphone permission on first record
- [ ] Welcome message plays when "Start Recording" clicked
- [ ] Transcript appears in real-time as you speak
- [ ] AI responds after ~1.8 seconds of silence
- [ ] AI feedback is contextual (different based on content)
- [ ] Can do multiple sessions by clicking Record again
- [ ] App gracefully handles edge cases (empty text, etc.)
- [ ] Audio properly recorded and saved

---

## Common Issues & Solutions

| Issue | Solution |
|-------|----------|
| No sound from AI | Check `isTtsReady` flag, ensure TTS initialized |
| AI doesn't respond | Check silence threshold timing, review keyword matching |
| Text keeps repeating | Use `fullTranscript.setLength(0)` to clear on new session |
| Permission denied | Ensure manifest has RECORD_AUDIO permission |
| Speech recognition keeps restarting | Check `onError()` handler delay |

---

## Next Steps for Enhancement

1. **Backend Integration:** Send transcript + feedback to server
2. **Analytics:** Track keyword frequencies, session duration
3. **Machine Learning:** Use actual AI API (GPT, etc.) instead of hard-coded responses
4. **Multi-language:** Support Spanish, Mandarin, etc.
5. **Recording Playback:** Allow users to review their recorded pitch
6. **Coaching Tips:** Add dynamic tips based on detected weaknesses

---

**Last Updated:** April 2026
**For Questions:** Contact the development team
