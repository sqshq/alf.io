--
-- This file is part of alf.io.
--
-- alf.io is free software: you can redistribute it and/or modify
-- it under the terms of the GNU General Public License as published by
-- the Free Software Foundation, either version 3 of the License, or
-- (at your option) any later version.
--
-- alf.io is distributed in the hope that it will be useful,
-- but WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
-- GNU General Public License for more details.
--
-- You should have received a copy of the GNU General Public License
-- along with alf.io.  If not, see <http://www.gnu.org/licenses/>.
--

CREATE TEMPORARY TABLE IF NOT EXISTS migration_14_1_7_3 AS (SELECT * FROM sponsor_scan);
drop table sponsor_scan;
create table sponsor_scan (
    id integer auto_increment primary key not null,
    creation timestamp  not null,
    event_id integer not null,
    ticket_id integer not null,
    user_id integer NOT NULL DEFAULT 0
) ENGINE=InnoDB CHARACTER SET=utf8 COLLATE utf8_bin;
-- constraints
alter table sponsor_scan add constraint spsc_unique_ticket unique(event_id, ticket_id, user_id);
alter table sponsor_scan add foreign key(event_id) references event(id);
alter table sponsor_scan add foreign key(ticket_id) references ticket(id);
insert into sponsor_scan select * from migration_14_1_7_3;
drop table migration_14_1_7_3;