package hobbyinvestor.repository;

import hobbyinvestor.model.StockSymbol;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockSymbolRepository extends CrudRepository<StockSymbol, String> {
}