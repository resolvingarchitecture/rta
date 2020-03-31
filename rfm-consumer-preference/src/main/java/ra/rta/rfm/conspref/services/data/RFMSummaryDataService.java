package ra.rta.rfm.conspref.services.data;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import ra.rta.rfm.conspref.models.Group;
import ra.rta.rfm.conspref.models.KPICustomerSummary;
import ra.rta.rfm.conspref.models.KPIGroupSummary;

import java.util.UUID;

public class RFMSummaryDataService extends BaseDataService {

    RFMSummaryDataService(Session session) {
        super(session);
    }

    public KPIGroupSummary getLatestKPIPartnerSummary(Group group, int termcode) {
        KPIGroupSummary summary = new KPIGroupSummary();
        summary.setPartner(group);
        summary.setTermcode(termcode);
        ResultSet rs = session.execute(new SimpleStatement("SELECT * FROM " + group.getName() + ".partner_KPI_rfm_windowed WHERE termcode = " + termcode + " limit 1;"));
        Row row = rs.one();
        if (row != null) {
            summary.setDate(row.getInt("date"));
            summary.setWindowDays(row.getInt("window_days"));
            summary.setRecencyEarliest(row.getInt("recency_earliest"));
            summary.setRecencyLatest(row.getInt("recency_latest"));
            summary.setRecencyBucket2Floor(row.getInt("recency_bucket_2_floor"));
            summary.setRecencyBucket3Floor(row.getInt("recency_bucket_3_floor"));
            summary.setFrequencyLeast(row.getLong("frequency_least"));
            summary.setFrequencyMost(row.getLong("frequency_most"));
            summary.setFrequencyBucket2Floor(row.getLong("frequency_bucket_2_floor"));
            summary.setFrequencyBucket3Floor(row.getLong("frequency_bucket_3_floor"));
            summary.setMonetaryLeast(row.getDouble("monetary_least"));
            summary.setMonetaryMost(row.getDouble("monetary_most"));
            summary.setMonetaryBucket2Floor(row.getDouble("monetary_bucket_2_floor"));
            summary.setMonetaryBucket3Floor(row.getDouble("monetary_bucket_3_floor"));
        }
        return summary;
    }

    public KPIGroupSummary getKPIPartnerSummaryByDate(Group group, int termcode, Integer date) {
        KPIGroupSummary summary = new KPIGroupSummary();
        summary.setPartner(group);
        summary.setTermcode(termcode);
        summary.setDate(date);
        ResultSet rs = session.execute(new SimpleStatement("SELECT * FROM " + group.getName() + ".partner_KPI_rfm_windowed WHERE termcode = " + termcode + " and date="+date+";"));
        Row row = rs.one();
        if (row != null) {
            summary.setWindowDays(row.getInt("window_days"));
            summary.setRecencyEarliest(row.getInt("recency_earliest"));
            summary.setRecencyLatest(row.getInt("recency_latest"));
            summary.setRecencyBucket2Floor(row.getInt("recency_bucket_2_floor"));
            summary.setRecencyBucket3Floor(row.getInt("recency_bucket_3_floor"));
            summary.setFrequencyLeast(row.getLong("frequency_least"));
            summary.setFrequencyMost(row.getLong("frequency_most"));
            summary.setFrequencyBucket2Floor(row.getLong("frequency_bucket_2_floor"));
            summary.setFrequencyBucket3Floor(row.getLong("frequency_bucket_3_floor"));
            summary.setMonetaryLeast(row.getDouble("monetary_least"));
            summary.setMonetaryMost(row.getDouble("monetary_most"));
            summary.setMonetaryBucket2Floor(row.getDouble("monetary_bucket_2_floor"));
            summary.setMonetaryBucket3Floor(row.getDouble("monetary_bucket_3_floor"));
        }
        return summary;
    }

    public KPICustomerSummary getLatestKPICustomerSummary(Group group, UUID adid, int termcode) {
        KPICustomerSummary summary = new KPICustomerSummary();
        summary.setPartner(group);
        summary.setAdId(adid);
        summary.setTermcode(termcode);
        ResultSet rs = session.execute(new SimpleStatement("SELECT date, frequency, monetary, recency FROM " + group.getName() + ".customer_KPI_rfm_windowed WHERE adid = " + adid + " AND termcode = " + termcode + " limit 1;"));
        Row row = rs.one();
        if (row != null) {
            summary.setDate(row.getInt("date"));
            summary.setRecency(row.getInt("recency"));
            summary.setFrequency(row.getLong("frequency"));
            summary.setMonetary(row.getDouble("monetary"));
        }
        return summary;
    }

    public KPICustomerSummary getKPICustomerSummaryByDate(Group group, UUID adic, int termcode, Integer date) {
        KPICustomerSummary summary = new KPICustomerSummary();
        summary.setPartner(group);
        summary.setAdId(adic);
        summary.setTermcode(termcode);
        summary.setDate(date);
        summary.setRecency(date);
        ResultSet rs = session.execute(new SimpleStatement("SELECT frequency, monetary FROM " + group.getName() + ".customer_KPI_rfm_windowed WHERE adic = " + adic + " AND termcode = " + termcode + " and date = " + date + ";"));
        Row row = rs.one();
        if (row != null) {
            summary.setFrequency(row.getLong("frequency"));
            summary.setMonetary(row.getDouble("monetary"));
        }
        return summary;
    }
}