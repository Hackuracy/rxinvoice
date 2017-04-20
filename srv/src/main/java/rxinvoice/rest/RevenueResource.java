package rxinvoice.rest;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import restx.annotations.GET;
import restx.annotations.RestxResource;
import restx.factory.Component;
import rxinvoice.AppModule;
import rxinvoice.domain.Company;
import rxinvoice.domain.Invoice;
import rxinvoice.domain.Revenue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestxResource
@Component
public class RevenueResource {
    private final InvoiceResource invoiceResource;
    private final CompanyResource companyResource;

    public RevenueResource(InvoiceResource invoiceResource, CompanyResource companyResource) {
        this.invoiceResource = invoiceResource;
        this.companyResource = companyResource;
    }

    @GET("/revenues/fiscal")
    public Iterable<Revenue> getFiscalYearRevenues() {
        Optional<Company> company = companyResource.findCompanyByKey(AppModule.currentUser().getCompanyRef());
        Company.FiscalYear fiscalYear =
                company.isPresent() ? company.get().getFiscalYear().current() : Company.FiscalYear.DEFAULT;
        Company.FiscalYear previousFiscalYear = fiscalYear.previous();
        Company.FiscalYear nextFiscalYear = fiscalYear.next();

        return Arrays.asList(
                computeRevenue(previousFiscalYear.getStart(), previousFiscalYear.getEnd(), Revenue.PeriodType.YEARLY),
                computeRevenue(fiscalYear.getStart(), fiscalYear.getEnd(), Revenue.PeriodType.YEARLY),
                computeRevenue(nextFiscalYear.getStart(), nextFiscalYear.getEnd(), Revenue.PeriodType.YEARLY));
    }

    @GET("/revenues/monthly")
    public Iterable<Revenue> getMonthlyRevenues(Optional<DateTime> from, Optional<DateTime> to) {
        DateTime fromMonth = from.or(DateTime.now().minusMonths(6)).withDayOfMonth(1);
        DateTime toMonth = to.or(DateTime.now());
        List<Revenue> revenues = new ArrayList<>();

        while (fromMonth.isBefore(toMonth)) {
            revenues.add(computeRevenue(fromMonth, fromMonth.plusMonths(1).minusDays(1), Revenue.PeriodType.MONTHLY));
            fromMonth = fromMonth.plusMonths(1);
        }

        return revenues;
    }

    private Revenue computeRevenue(DateTime from, DateTime to, Revenue.PeriodType periodType) {
        Revenue revenue = new Revenue()
                .setFrom(from)
                .setTo(to)
                .setPeriodType(periodType);
        Iterable<Invoice> invoices = invoiceResource.findInvoicesByDates(from.toLocalDate().toString(), to.toLocalDate().toString());
        for (Invoice invoice : invoices) {
            switch (invoice.getStatus()) {
                case PAID:
                    revenue.setPaid(revenue.getPaid().add(invoice.getGrossAmount()));
                    break;
                default:
                    revenue.setInvoiced(revenue.getInvoiced().add(invoice.getGrossAmount()));
                    break;
            }
        }

        return revenue;
    }
}
