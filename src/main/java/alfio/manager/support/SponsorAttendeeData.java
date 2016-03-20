/**
 * This file is part of alf.io.
 *
 * alf.io is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * alf.io is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with alf.io.  If not, see <http://www.gnu.org/licenses/>.
 */
package alfio.manager.support;

import alfio.model.TicketFieldNameAndValue;
import lombok.Data;

import java.util.List;

@Data
public class SponsorAttendeeData {
    /**
     * The ticket UUID
     */
    private final String ticketId;
    /**
     * UTC Scanning timestamp, pattern: yyyy-MM-ddTHH:mm:ssZ
     */
    private final String timestamp;
    /**
     * Attendee's full name, as specified during the reservation
     */
    private final String fullName;
    /**
     * Attendee's e-mail
     */
    private final String email;

    /**
     * Additional (optional) info, if any, as specified on ticket assignment form
     */
    private final List<TicketFieldNameAndValue> optionalData;
}
