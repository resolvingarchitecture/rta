package ra.rta.services.data;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.datastax.driver.core.*;
import ra.rta.models.Account;
import ra.rta.models.Customer;
import ra.rta.models.KPI;
import ra.rta.models.utilities.DateUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: Swap internals with Spring Cassandra?
 */
public class CustomerDataService extends BaseDataService {

    private static Logger LOG = LoggerFactory.getLogger(CustomerDataService.class);

	public CustomerDataService(Session session) {
		super(session);
	}

	public void saveKPI(Customer customer, Account account, boolean transactional) throws Exception {
        List<ResultSetFuture> futures = new ArrayList<>();
        ResultSetFuture future;
		for(KPI KPI : customer.getKPIS()) {
			if (transactional) {
				int windowDaysLeft = KPI.getWindowDays() - (int) DateUtility.daysDifference(Calendar.getInstance().getTime(), KPI.getDate());
				if (KPI.getActive() && windowDaysLeft > 0) { // Only insert if made active and not already expired
					Calendar nowCal = Calendar.getInstance();
					nowCal.add(Calendar.SECOND, windowDaysLeft);
					String expiresDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(nowCal.getTime());
                    long ttl = windowDaysLeft * 24 * 60 * 60; // In seconds
                    // Add to windowed Customer KPI
					future = session.executeAsync(new SimpleStatement("INSERT INTO " + customer.getPartner().getName() + ".customer_KPI_active (adic, termcode, expires, window_days) VALUES ("
                            + customer.getAdId() + "," + KPI.getTermcode() + ",'" + expiresDate + "', " + KPI.getWindowDays() + ") USING TTL " + ttl + ";"));
                    futures.add(future);
				}

                // Update Cassandra frequency & monetary counters (recency assumed by transaction date used in counters)
                future = session.executeAsync(new SimpleStatement("UPDATE " + customer.getPartner().getName() + ".customer_KPI_frequency SET frequency = frequency + 1 WHERE adic = "+ KPI.getAdId()+" AND termcode = "+ KPI.getTermcode()+" AND date = "+DateUtility.dateToInt(KPI.getDate())+";"));
                futures.add(future);
                future = session.executeAsync(new SimpleStatement("UPDATE " + customer.getPartner().getName() + ".customer_KPI_monetary SET monetary = monetary + " + ((int)(KPI.getMonetary()*100)) + " WHERE adic = "+ KPI.getAdId()+" AND termcode = "+ KPI.getTermcode()+" AND date = "+DateUtility.dateToInt(KPI.getDate())+";"));
                futures.add(future);
			} else {
                // Add to Customer KPI - non expiring
				future = session.executeAsync(new SimpleStatement("INSERT INTO " + customer.getPartner().getName() + ".customer_KPI (adic, termcode) VALUES (" + customer.getAdId() + "," + KPI.getTermcode() + ");"));
			    futures.add(future);
            }
            // Add to historical Customer KPI
            future = session.executeAsync(new SimpleStatement("INSERT INTO " + customer.getPartner().getName() + ".customer_KPI_history (adic, termcode, transaction_date, uaic, KPI_type_id, recency, recency_earliest_global, recency_latest_global, recency_score, frequency, frequency_total, frequency_least_global, frequency_most_global, frequency_score, monetary, monetary_total, monetary_least_global, monetary_most_global, monetary_score, window_days, active) VALUES ("
                    + KPI.getAdId() + ", " + KPI.getTermcode()+", '"+DateUtility.timestampToString(KPI.getDate())+"', '" + account.getaId() +"', " + KPI.getType().ordinal() +", " + KPI.getRecency() + ", " + KPI.getKPIPartnerSummary().getRecencyEarliest() + ", " + KPI.getKPIPartnerSummary().getRecencyLatest() + ", " + KPI.getRecencyScore() + ", " + KPI.getFrequency() + ", " + KPI.getKPICustomerSummary().getFrequency() + ", " + KPI.getKPIPartnerSummary().getFrequencyLeast() + ", " + KPI.getKPIPartnerSummary().getFrequencyMost() + ", " + KPI.getFrequencyScore() + ", " + KPI.getMonetary() + ", " + KPI.getKPICustomerSummary().getMonetary() + ", " + KPI.getKPIPartnerSummary().getMonetaryLeast() + ", " + KPI.getKPIPartnerSummary().getMonetaryMost() + ", " + KPI.getMonetaryScore() + ", " + KPI.getWindowDays() + ", " + KPI.getActive()+");"));
		    futures.add(future);
        }
        for(ResultSetFuture rsf : futures) {
            rsf.getUninterruptibly();
        }
	}

	public void save(Customer customer) throws Exception {
		if ("create".equals(customer.getSave()) || "update".equals(customer.getSave())) {
			session.execute(new SimpleStatement("INSERT INTO " + customer.getPartner().getName()
                    + ".customer (adic, uhic, open_date, open_date_months, close_date, process_date, bank_branch, status, country, zip_code, age, birth_year, deceased_year, type, state, gender, marital_status, investable_assets, estatement_indicator, glba_opt_out, solicitation_code, segmentation, business_inception_date, business_sic_code, business_naics_code, business_annual_sales, business_num_employees, save) VALUES ("
                    +customer.getAdId() + ", '" + customer.getUhId() + "', "+DateUtility.timestampToString(customer.getOpenDate())+", "+customer.getOpenDateMonths()+", "
                    +DateUtility.timestampToString(customer.getCloseDate())+", "+DateUtility.timestampToString(customer.getProcessDate())+", '"+customer.getBankBranch()+"', "+customer.getStatus().ordinal()+", "
                    +customer.getCountry()+", '"+customer.getZipCode()+"', "+customer.getAge()+", "+customer.getBirthYear()+", "
                    +customer.getDeceasedYear()+", '"+customer.getType().ordinal()+"', '"+customer.getState()+"', "+customer.getGender().ordinal()+", "
                    +customer.getMaritalStatus().ordinal()+", "+customer.getInvestableAssets()+", "+customer.getEstatementIndicator()+", "
                    +customer.getGlbaOptOut()+", "+customer.getSolicitationCode()+", "+customer.getSegmentation()+", "
                    +DateUtility.timestampToString(customer.getBusinessInceptionDate())+", "+customer.getBusinessSicCode()+", "+customer.getBusinessNaicsCode()+", "
                    +customer.getBusinessAnnualSales()+", "+customer.getBusinessNumEmployees()+", '"+customer.getSave()+"');"));
		}
	}

    public void loadCustomer(Customer customer, String processDate) throws Exception {
        ResultSet rs = session.execute(new SimpleStatement("SELECT * FROM "+customer.getPartner().getName()+".customer WHERE adic="+customer.getAdId()+" and process_date='" + processDate + "' limit 1;"));
        Row row = rs.one();
        if(row != null){
            customer.setUhId(row.getString("uhic"));
            customer.setOpenDate(new Date(row.getDate("open_date").getMillisSinceEpoch()));
            customer.setCloseDate(new Date(row.getDate("close_date").getMillisSinceEpoch()));
            customer.setProcessDate(new Date(row.getDate("process_date").getMillisSinceEpoch()));
            customer.setBankBranch(row.getString("bank_branch"));
            customer.setStatus(Customer.Status.values()[(row.getInt("status"))]);
            customer.setCountry(row.getInt("country"));
            customer.setZipCode(row.getString("zip_code"));
            customer.setBirthYear(row.getInt("birth_year"));
            customer.setDeceasedYear(row.getInt("deceased_year"));
            customer.setType(Customer.Type.values()[(row.getInt("type"))]);
            customer.setState(row.getString("state"));
            customer.setEthnicity(Customer.Ethnicity.values()[(row.getInt("ethnicity"))]);
            customer.setGender(Customer.Gender.values()[(row.getInt("gender"))]);
            customer.setMaritalStatus(Customer.MaritalStatus.values()[(row.getInt("marital_status"))]);
            customer.setInvestableAssets(row.getDouble("investable_assets"));
            customer.setEstatementIndicator(row.getBool("estatement_indicator"));
            customer.setGlbaOptOut(row.getBool("glba_opt_out"));
            customer.setSolicitationCode(row.getBool("solicitation_code"));
            customer.setSegmentation(row.getInt("segmentation"));
            customer.setBusinessInceptionDate(new Date(row.getDate("business_inception_date").getMillisSinceEpoch()));
            customer.setBusinessSicCode(row.getInt("business_sic_code"));
            customer.setBusinessNaicsCode(row.getInt("business_naics_code"));
            customer.setBusinessAnnualSales(row.getDouble("business_annual_sales"));
            customer.setBusinessNumEmployees(row.getInt("business_num_employees"));
            customer.setSubType1(row.getString("sub_type1"));
            customer.setSubType2(row.getString("sub_type2"));
            customer.setSubType3(row.getString("sub_type3"));
        }
    }
}