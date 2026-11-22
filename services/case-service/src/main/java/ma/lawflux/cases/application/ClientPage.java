package ma.lawflux.cases.application;

import java.util.List;
import ma.lawflux.cases.domain.Client;

public record ClientPage(
        List<Client> items,
        int page,
        int pageSize,
        long totalElements
) {
    public ClientPage {
        items = List.copyOf(items);
    }

    public long totalPages() {
        return totalElements == 0 ? 0 : (totalElements + pageSize - 1) / pageSize;
    }
}