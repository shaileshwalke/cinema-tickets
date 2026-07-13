package uk.gov.dwp.uc.pairtest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

public class TicketServiceImplTest {

    private static final Long VALID_ACCOUNT_ID = 1L;

    @Mock
    private TicketPaymentService ticketPaymentService;

    @Mock
    private SeatReservationService seatReservationService;

    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ticketService = new TicketServiceImpl(ticketPaymentService, seatReservationService);
    }

    @Nested
    class CoreCalculation {

        @Test
        void adultOnlyPurchase_chargesCorrectAmountAndReservesOneSeat() {
            ticketService.purchaseTickets(VALID_ACCOUNT_ID, new TicketTypeRequest(Type.ADULT, 1));

            verify(ticketPaymentService).makePayment(VALID_ACCOUNT_ID, 25);
            verify(seatReservationService).reserveSeat(VALID_ACCOUNT_ID, 1);
        }

        @Test
        void mixedOrder_chargesAndReservesCorrectly() {
            ticketService.purchaseTickets(VALID_ACCOUNT_ID,
                    new TicketTypeRequest(Type.ADULT, 2),
                    new TicketTypeRequest(Type.CHILD, 3),
                    new TicketTypeRequest(Type.INFANT, 1));

            // (2 * £25) + (3 * £15) + (1 * £0) = £95
            verify(ticketPaymentService).makePayment(VALID_ACCOUNT_ID, 95);
            // Infants don't get a seat: 2 adults + 3 children = 5 seats
            verify(seatReservationService).reserveSeat(VALID_ACCOUNT_ID, 5);
        }

        @Test
        void repeatedRequestsOfSameType_areAggregated() {
            ticketService.purchaseTickets(VALID_ACCOUNT_ID,
                    new TicketTypeRequest(Type.ADULT, 1),
                    new TicketTypeRequest(Type.ADULT, 1),
                    new TicketTypeRequest(Type.CHILD, 2));

            verify(ticketPaymentService).makePayment(VALID_ACCOUNT_ID, 80);
            verify(seatReservationService).reserveSeat(VALID_ACCOUNT_ID, 4);
        }
    }

    @Nested
    class AdultRequiredForChildOrInfant {

        @Test
        void childTicketWithoutAnAdultIsRejected() {
            assertThrows(InvalidPurchaseException.class, () ->
                    ticketService.purchaseTickets(VALID_ACCOUNT_ID, new TicketTypeRequest(Type.CHILD, 1)));
            verifyNoInteractions(ticketPaymentService, seatReservationService);
        }

        @Test
        void infantTicketWithoutAnAdultIsRejected() {
            assertThrows(InvalidPurchaseException.class, () ->
                    ticketService.purchaseTickets(VALID_ACCOUNT_ID, new TicketTypeRequest(Type.INFANT, 1)));
            verifyNoInteractions(ticketPaymentService, seatReservationService);
        }
    }
}
