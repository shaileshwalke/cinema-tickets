package uk.gov.dwp.uc.pairtest;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

public class TicketServiceImpl implements TicketService {
    /**
     * Should only have private methods other than the one below.
     */

    private static final int MAX_TICKETS_PER_PURCHASE = 25;

    private static final Map<Type, Integer> TICKET_PRICES = new EnumMap<>(Type.class);
    static {
        TICKET_PRICES.put(Type.INFANT, 0);
        TICKET_PRICES.put(Type.CHILD, 15);
        TICKET_PRICES.put(Type.ADULT, 25);
    }

    private final TicketPaymentService ticketPaymentService;
    private final SeatReservationService seatReservationService;

    public TicketServiceImpl(TicketPaymentService ticketPaymentService,
                             SeatReservationService seatReservationService) {
        this.ticketPaymentService = Objects.requireNonNull(ticketPaymentService, "ticketPaymentService must not be null");
        this.seatReservationService = Objects.requireNonNull(seatReservationService, "seatReservationService must not be null");
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        validateAccountId(accountId);

        TicketCounts ticketCounts = aggregateTicketCounts(ticketTypeRequests);
        validatePurchaseRules(ticketCounts);

        ticketPaymentService.makePayment(accountId, ticketCounts.totalAmount());
        seatReservationService.reserveSeat(accountId, ticketCounts.totalSeats());
    }

    private void validateAccountId(Long accountId) {
        if (accountId == null || accountId <= 0) {
            throw new InvalidPurchaseException("Account id must be greater than zero");
        }
    }

    private TicketCounts aggregateTicketCounts(TicketTypeRequest[] ticketTypeRequests) {
        if (ticketTypeRequests == null || ticketTypeRequests.length == 0) {
            throw new InvalidPurchaseException("At least one ticket type request must be provided");
        }

        Map<Type, Integer> counts = new EnumMap<>(Type.class);
        for (Type type : Type.values()) {
            counts.put(type, 0);
        }
        for (TicketTypeRequest request : ticketTypeRequests) {
            if (request == null) {
                throw new InvalidPurchaseException("Ticket type request must not be null");
            }
            if (request.getNoOfTickets() < 0) {
                throw new InvalidPurchaseException("Number of tickets cannot be negative");
            }
            counts.merge(request.getTicketType(), request.getNoOfTickets(), Integer::sum);
        }
        return new TicketCounts(counts.get(Type.ADULT), counts.get(Type.CHILD), counts.get(Type.INFANT));
    }

    private void validatePurchaseRules(TicketCounts ticketCounts) {
        validateAtLeastOneTicketRequested(ticketCounts);
        validateMaximumTicketsNotExceeded(ticketCounts);
        validateAdultTicketPresentForChildOrInfant(ticketCounts);
        validateInfantsDoNotOutnumberAdults(ticketCounts);
    }

    private void validateAtLeastOneTicketRequested(TicketCounts ticketCounts) {
        if (ticketCounts.total() == 0) {
            throw new InvalidPurchaseException("At least one ticket must be purchased");
        }
    }

    private void validateMaximumTicketsNotExceeded(TicketCounts ticketCounts) {
        if (ticketCounts.total() > MAX_TICKETS_PER_PURCHASE) {
            throw new InvalidPurchaseException(
                    "Cannot purchase more than " + MAX_TICKETS_PER_PURCHASE + " tickets at a time");
        }
    }

    private void validateAdultTicketPresentForChildOrInfant(TicketCounts ticketCounts) {
        boolean childOrInfantRequested = ticketCounts.child() > 0 || ticketCounts.infant() > 0;
        if (ticketCounts.adult() == 0 && childOrInfantRequested) {
            throw new InvalidPurchaseException(
                    "Child and Infant tickets cannot be purchased without purchasing an Adult ticket");
        }
    }

    private void validateInfantsDoNotOutnumberAdults(TicketCounts ticketCounts) {
        // Assumption "infants will be sitting on an Adult's lap": each infant
        //needs an adult's lap to sit on, so there can never be more infants than
        if (ticketCounts.infant() > ticketCounts.adult()) {
            throw new InvalidPurchaseException(
                    "Each infant ticket requires an accompanying Adult ticket (infants sit on an adult's lap)");
        }
    }

    /**
     * Ticket counts per type, plus the price/seat totals derived from them -
     * avoids passing a raw {@code Map<Type, Integer>} around and repeatedly
     * calling {@code .get(Type.X)}.
     */
    private record TicketCounts(int adult, int child, int infant) {

        int total() {
            return adult + child + infant;
        }

        int totalAmount() {
            return adult * TICKET_PRICES.get(Type.ADULT)
                    + child * TICKET_PRICES.get(Type.CHILD)
                    + infant * TICKET_PRICES.get(Type.INFANT);
        }

        int totalSeats() {
            // Infants are not allocated a seat - they sit on an adult's lap.
            return adult + child;
        }
    }

}
