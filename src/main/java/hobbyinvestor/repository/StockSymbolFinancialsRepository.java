package hobbyinvestor.repository;

import hobbyinvestor.model.StockSymbolFinancials;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockSymbolFinancialsRepository extends CrudRepository<StockSymbolFinancials, String> {
}