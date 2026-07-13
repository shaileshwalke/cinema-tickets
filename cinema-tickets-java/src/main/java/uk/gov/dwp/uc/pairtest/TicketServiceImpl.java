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
        Map<Type, Integer> ticketCounts = aggregateTicketCounts(ticketTypeRequests);

        int totalAmountToPay = calculateTotalAmount(ticketCounts);
        int totalSeatsToAllocate = calculateSeatsToAllocate(ticketCounts);

        ticketPaymentService.makePayment(accountId, totalAmountToPay);
        seatReservationService.reserveSeat(accountId, totalSeatsToAllocate);
    }

    private Map<Type, Integer> aggregateTicketCounts(TicketTypeRequest[] ticketTypeRequests) {
        Map<Type, Integer> counts = new EnumMap<>(Type.class);
        for (Type type : Type.values()) {
            counts.put(type, 0);
        }
        for (TicketTypeRequest request : ticketTypeRequests) {
            counts.merge(request.getTicketType(), request.getNoOfTickets(), Integer::sum);
        }
        return counts;
    }

    private int calculateTotalAmount(Map<Type, Integer> ticketCounts) {
        int total = 0;
        for (Map.Entry<Type, Integer> entry : ticketCounts.entrySet()) {
            total += TICKET_PRICES.get(entry.getKey()) * entry.getValue();
        }
        return total;
    }

    private int calculateSeatsToAllocate(Map<Type, Integer> ticketCounts) {
        // Infants are not allocated a seat - they sit on an adult's lap.
        return ticketCounts.get(Type.ADULT) + ticketCounts.get(Type.CHILD);
    }

}
