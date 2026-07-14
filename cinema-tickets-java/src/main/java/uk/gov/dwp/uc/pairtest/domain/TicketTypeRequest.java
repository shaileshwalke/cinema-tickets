package uk.gov.dwp.uc.pairtest.domain;

import java.util.Objects;

/**
 * Immutable Object.
 */
public final class TicketTypeRequest {

    private final Type type;
    private final int noOfTickets;

    public TicketTypeRequest(Type type, int noOfTickets) {
        this.type = type;
        this.noOfTickets = noOfTickets;
    }

    public int getNoOfTickets() {
        return noOfTickets;
    }

    public Type getTicketType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TicketTypeRequest)) return false;
        TicketTypeRequest that = (TicketTypeRequest) o;
        return noOfTickets == that.noOfTickets && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, noOfTickets);
    }

    @Override
    public String toString() {
        return "TicketTypeRequest{type=" + type + ", noOfTickets=" + noOfTickets + '}';
    }

    public enum Type {
        ADULT, CHILD, INFANT
    }

}
