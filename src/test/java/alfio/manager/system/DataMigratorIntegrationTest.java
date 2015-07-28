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
package alfio.manager.system;

import alfio.TestConfiguration;
import alfio.config.DataSourceConfiguration;
import alfio.config.Initializer;
import alfio.manager.EventManager;
import alfio.manager.user.UserManager;
import alfio.model.Event;
import alfio.model.Ticket;
import alfio.model.modification.DateTimeModification;
import alfio.model.modification.EventModification;
import alfio.model.modification.TicketCategoryModification;
import alfio.model.modification.support.LocationDescriptor;
import alfio.model.system.EventMigration;
import alfio.model.user.Organization;
import alfio.repository.EventRepository;
import alfio.repository.TicketRepository;
import alfio.repository.system.EventMigrationRepository;
import alfio.repository.user.OrganizationRepository;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {DataSourceConfiguration.class, TestConfiguration.class})
@ActiveProfiles(Initializer.PROFILE_DEV)
@WebIntegrationTest
public class DataMigratorIntegrationTest {

    public static final int AVAILABLE_SEATS = 20;

    @Autowired
    private EventManager eventManager;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private UserManager userManager;
    @Autowired
    private TicketRepository ticketRepository;
    @Autowired
    private DataMigrator dataMigrator;
    @Autowired
    private EventMigrationRepository eventMigrationRepository;
    @Value("${alfio.version}")
    private String currentVersion;
    @Value("${alfio.build-ts}")
    private String buildTimestamp;

    @BeforeClass
    public static void initEnv() {
        System.setProperty("datasource.dialect", "HSQLDB");
        System.setProperty("datasource.driver", "org.hsqldb.jdbcDriver");
        System.setProperty("datasource.url", "jdbc:hsqldb:mem:alfio");
        System.setProperty("datasource.username", "sa");
        System.setProperty("datasource.password", "");
        System.setProperty("datasource.validationQuery", "SELECT 1 FROM INFORMATION_SCHEMA.SYSTEM_USERS");
    }

    private Pair<Event, String> initEvent(List<TicketCategoryModification> categories) {
        return initEvent(categories, "display name");
    }

    private Pair<Event,String> initEvent(List<TicketCategoryModification> categories, String displayName) {
        String organizationName = UUID.randomUUID().toString();
        String username = UUID.randomUUID().toString();
        String eventName = UUID.randomUUID().toString();

        organizationRepository.create(organizationName, "org", "email@example.com");
        Organization organization = organizationRepository.findByName(organizationName).get(0);
        userManager.insertUser(organization.getId(), username, "test", "test", "test@example.com");

        EventModification em = new EventModification(null, "url", "url", "url", null,
                eventName, displayName, organization.getId(),
                "muh location", "muh description",
                new DateTimeModification(LocalDate.now().plusDays(5), LocalTime.now()),
                new DateTimeModification(LocalDate.now().plusDays(5), LocalTime.now().plusHours(1)),
                BigDecimal.TEN, "CHF", AVAILABLE_SEATS, BigDecimal.ONE, true, null, categories, false, new LocationDescriptor("","","",""));
        eventManager.createEvent(em);
        return Pair.of(eventManager.getSingleEvent(eventName, username), username);
    }

    @Test
    public void testMigration() {
        List<TicketCategoryModification> categories = Collections.singletonList(
                new TicketCategoryModification(null, "default", AVAILABLE_SEATS,
                        new DateTimeModification(LocalDate.now(), LocalTime.now()),
                        new DateTimeModification(LocalDate.now(), LocalTime.now()),
                        "desc", BigDecimal.TEN, false, "", false));
        Event event = initEvent(categories).getKey();

        eventRepository.updatePrices(1000, "CHF", 40, false, BigDecimal.ONE, "STRIPE", event.getId());

        dataMigrator.migrateEventsToCurrentVersion();
        EventMigration eventMigration = eventMigrationRepository.loadEventMigration(event.getId());
        assertNotNull(eventMigration);
        assertEquals(buildTimestamp, eventMigration.getBuildTimestamp().toString());
        assertEquals(currentVersion, eventMigration.getCurrentVersion());

        List<Ticket> tickets = ticketRepository.findFreeByEventId(event.getId());
        assertNotNull(tickets);
        assertFalse(tickets.isEmpty());
        assertEquals(40, tickets.size());
        assertTrue(tickets.stream().allMatch(t -> t.getCategoryId() == null));
    }

    @Test
    public void testMigrationWithExistingRecord() {
        List<TicketCategoryModification> categories = Collections.singletonList(
                new TicketCategoryModification(null, "default", AVAILABLE_SEATS,
                        new DateTimeModification(LocalDate.now(), LocalTime.now()),
                        new DateTimeModification(LocalDate.now(), LocalTime.now()),
                        "desc", BigDecimal.TEN, false, "", false));
        Event event = initEvent(categories).getKey();

        eventMigrationRepository.insertMigrationData(event.getId(), "1.4", ZonedDateTime.now(ZoneId.of("UTC")).minusDays(1), EventMigration.Status.COMPLETE.toString());
        eventRepository.updatePrices(1000, "CHF", 40, false, BigDecimal.ONE, "STRIPE", event.getId());
        dataMigrator.migrateEventsToCurrentVersion();
        EventMigration eventMigration = eventMigrationRepository.loadEventMigration(event.getId());
        assertNotNull(eventMigration);
        assertEquals(buildTimestamp, eventMigration.getBuildTimestamp().toString());
        assertEquals(currentVersion, eventMigration.getCurrentVersion());

        List<Ticket> tickets = ticketRepository.findFreeByEventId(event.getId());
        assertNotNull(tickets);
        assertFalse(tickets.isEmpty());
        assertEquals(40, tickets.size());
        assertTrue(tickets.stream().allMatch(t -> t.getCategoryId() == null));
    }

    @Test
    public void testAlreadyMigratedEvent() {
        List<TicketCategoryModification> categories = Collections.singletonList(
                new TicketCategoryModification(null, "default", AVAILABLE_SEATS,
                        new DateTimeModification(LocalDate.now(), LocalTime.now()),
                        new DateTimeModification(LocalDate.now(), LocalTime.now()),
                        "desc", BigDecimal.TEN, false, "", false));
        Event event = initEvent(categories).getKey();

        ZonedDateTime migrationTs = ZonedDateTime.now(ZoneId.of("UTC"));
        eventMigrationRepository.insertMigrationData(event.getId(), currentVersion, migrationTs, EventMigration.Status.COMPLETE.toString());
        eventRepository.updatePrices(1000, "CHF", 40, false, BigDecimal.ONE, "STRIPE", event.getId());
        dataMigrator.migrateEventsToCurrentVersion();
        EventMigration eventMigration = eventMigrationRepository.loadEventMigration(event.getId());
        assertNotNull(eventMigration);
        assertEquals(migrationTs.toString(), eventMigration.getBuildTimestamp().toString());
        assertEquals(currentVersion, eventMigration.getCurrentVersion());

        List<Ticket> tickets = ticketRepository.findFreeByEventId(event.getId());
        assertNotNull(tickets);
        assertFalse(tickets.isEmpty());
        assertEquals(AVAILABLE_SEATS, tickets.size());//<-- the migration has not been done
        assertTrue(tickets.stream().allMatch(t -> t.getCategoryId() == null));
    }

    @Test
    public void testUpdateDisplayName() {
        List<TicketCategoryModification> categories = Collections.singletonList(
                new TicketCategoryModification(null, "default", AVAILABLE_SEATS,
                        new DateTimeModification(LocalDate.now(), LocalTime.now()),
                        new DateTimeModification(LocalDate.now(), LocalTime.now()),
                        "desc", BigDecimal.TEN, false, "", false));
        Event event = initEvent(categories, null).getKey();

        dataMigrator.migrateEventsToCurrentVersion();
        EventMigration eventMigration = eventMigrationRepository.loadEventMigration(event.getId());
        assertNotNull(eventMigration);
        assertEquals(buildTimestamp, eventMigration.getBuildTimestamp().toString());
        assertEquals(currentVersion, eventMigration.getCurrentVersion());

        Event withDescription = eventRepository.findById(event.getId());
        assertNotNull(withDescription.getDisplayName());
        assertEquals(event.getShortName(), withDescription.getShortName());
        assertEquals(event.getShortName(), withDescription.getDisplayName());
    }
}