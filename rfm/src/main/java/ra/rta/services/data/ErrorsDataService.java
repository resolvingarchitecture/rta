package ra.rta.services.data;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import com.fasterxml.jackson.databind.ObjectMapper;
import ra.rta.models.Event;
import ra.rta.models.EventException;
import ra.rta.publish.cassandra.BaseDataService;

public class ErrorsDataService extends BaseDataService {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	ErrorsDataService(Session session) {
		super(session);
	}

	public void save(EventException exception, Event event) throws Exception {
		String partnerName = event.getEntity().getPartner().getName();
		String data = objectMapper.writeValueAsString(exception.getEvent().getEntity());
		SimpleStatement stmt = new SimpleStatement(
				"INSERT INTO "+partnerName+".errors (id, event_id, date, component, code, message, data) VALUES (?, ?, ?, ?, ?, ?, ?)");
		PreparedStatement pstmt = session.prepare(stmt);
		BoundStatement bstmt = pstmt.bind(exception.getId(), exception.getEvent().getId(), exception.getTimestamp(),
				exception.getComponent(), exception.getCode(), exception.getMessage(), data.trim());
		session.execute(bstmt);
	}
}
