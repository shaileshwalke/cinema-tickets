# Cinema Tickets

[![CI](https://github.com/shaileshwalke/cinema-tickets/actions/workflows/ci.yml/badge.svg)](https://github.com/shaileshwalke/cinema-tickets/actions/workflows/ci.yml)

Implementation of `TicketService` for the DWP cinema-tickets coding exercise
([exercise brief](https://github.com/dwp/cinema-tickets/tree/main/cinema-tickets-java)).

## Business rules

| Ticket type | Price | Gets a seat? |
|---|---|---|
| Infant | £0 | No - sits on an adult's lap |
| Child | £15 | Yes |
| Adult | £25 | Yes |

- Up to 25 tickets can be purchased in a single call.
- Child and Infant tickets cannot be purchased without at least one Adult ticket.
- Accounts with an id greater than zero are assumed valid and able to pay.

## How to run

```bash
mvn test      # unit tests only
mvn verify    # tests + Checkstyle + SpotBugs + JaCoCo coverage report
```

Coverage report after `mvn verify`: `target/site/jacoco/index.html`.

## Design

`purchaseTickets` is the only public method on `TicketServiceImpl`, as the
skeleton's Javadoc comment requires; everything else is private:

1. **`validateAccountId`** - `accountId` must be non-null and `> 0`.
2. **`aggregateTicketCounts`** - sums ticket counts per `Type` across every
   `TicketTypeRequest` passed in (so e.g. two separate `ADULT` requests in
   the same call are combined), rejecting `null` arrays/elements and
   negative counts along the way. Returns a small `TicketCounts` record
   rather than a raw `Map`.
3. **`validatePurchaseRules`** - a pipeline of four named, single-purpose
   checks: at least one ticket requested, no more than 25 in total, an
   Adult ticket present whenever Child/Infant tickets are requested, and
   infants not outnumbering adults (see *Assumptions* below).
4. **`TicketCounts.totalAmount()` / `totalSeats()`** - Infants are £0 and
   get no seat; Children are £15 with a seat; Adults are £25 with a seat.

If any validation fails, `InvalidPurchaseException` (now carrying a
descriptive message) is thrown *before* either third-party service is
called, so a rejected purchase never results in a partial payment or seat
reservation - covered explicitly by
`rejectedPurchaseNeverCallsEitherThirdPartyService`.

On success, `TicketPaymentService.makePayment` is called once with the
total price, then `SeatReservationService.reserveSeat` is called once with
the seat count - in that order, locked in by
`paymentIsMadeBeforeSeatsAreReserved`.

## Assumptions

- **Infants cannot outnumber adults.** Not one of the numbered business
  rules, but implied by *"infants... will be sitting on an Adult's lap"* -
  each infant needs a lap, so a purchase with (say) 1 adult and 2 infants
  is rejected. This is isolated in its own method
  (`validateInfantsDoNotOutnumberAdults`) with a comment flagging it as an
  interpretation, and it's the one rule I'd double check with a product
  owner before shipping - easy to delete if the brief is meant to be read
  literally.
- A request summing to zero tickets (e.g. a single `ADULT` request for
  `0` tickets) is rejected rather than silently treated as a no-op
  purchase.
- `accountId` validity is exactly "non-null and greater than zero", per
  the brief; no further checks (e.g. against a real account store) are in
  scope for this exercise.
