package kilian1111010.wealthandfinancetracker.domain.quote;

import kilian1111010.wealthandfinancetracker.domain.accounttype.AssetClass;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/quotes")
@RequiredArgsConstructor
class QuoteController {

    private final QuoteService quoteService;

    @GetMapping("/search")
    List<QuoteCandidate> search(@RequestParam String query, @RequestParam AssetClass assetClass) {
        return this.quoteService.search(query, assetClass);
    }
}
