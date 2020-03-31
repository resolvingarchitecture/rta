package ra.rta.rfm.conspref.models;

import ra.rta.EventException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class Record implements Serializable {

	static final long serialVersionUID = 1L;

	public String raw;
	public String rawEncoding;
	public Group group = new Group();
	public Customer customer = null;
	public FinancialTransaction financialTransaction;
	public List<EventException> eventErrors = new ArrayList<>();
	public boolean transformed = false;
	public String json;
	public Integer tried = 0;

}