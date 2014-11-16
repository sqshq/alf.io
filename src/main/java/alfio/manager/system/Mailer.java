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

import lombok.Data;
import org.springframework.core.io.InputStreamSource;
import org.springframework.scheduling.annotation.Async;

import java.util.Optional;

public interface Mailer {

	@Async
	void send(String to, String subject, String text, Optional<String> html, Attachment... attachment);

	@Data
	public class Attachment {
		private final String filename;
		private final InputStreamSource source;
		private final String contentType;
	}
}