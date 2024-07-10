package hobbyinvestor.repository;

import hobbyinvestor.model.StockSymbol;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockSymbolPagingRepository extends PagingAndSortingRepository<StockSymbol, String> {
}