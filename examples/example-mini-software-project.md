# Mini Example: Offline Emergency Messenger

This is a tiny example showing how specific context should feel.

## Project Overview Example

Konek.chat is an offline emergency messaging app for disaster situations in the Philippines.
It lets nearby users send 1:1 messages, room messages, and SOS alerts through Bluetooth
mesh-style communication when SIM, internet, and power infrastructure are unreliable.

## In Scope V1

- Local device identity
- 1:1 messages
- Room messages
- SOS broadcast
- Store-and-forward message queue
- No account required

## Out of Scope V1

- Payments
- Public social feed
- Cloud backup
- Centralized user profiles
- End-to-end production-grade mesh optimization

## Invariant Example

The app must never require an internet connection, SIM card, or account to send emergency
messages in the MVP.

## Unit Spec Example

First unit: local device identity.

Claude Code should implement only local device identity and should not touch messaging,
rooms, or SOS yet.

---

## Why This Level of Specificity Matters

Compare the invariant above with a generic one:

> Bad: "The app should work well offline."
> Good: "The app must never require an internet connection, SIM card, or account to send
> emergency messages in the MVP."

The first cannot be violated, because it cannot be checked. The second can be checked against
any diff in seconds, and it tells an agent exactly which shortcut is forbidden — adding a
sign-in wall, a server round trip, or a phone-number verification step.

Every section of the brain should be written so that a reviewer can point at a change and
say *this breaks it*, or *this does not*.
