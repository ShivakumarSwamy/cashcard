package example.cashcard;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/cashcards")
public class CashCardController {

    private final CashCardRepository cashCardRepository;

    public CashCardController(CashCardRepository cashCardRepository) {
        this.cashCardRepository = cashCardRepository;
    }

    @GetMapping("/{requestedId}")
    private ResponseEntity<CashCard> findById(@PathVariable Long requestedId, Principal principal) {
        CashCard cashCard = findCashCard(requestedId, principal);
        if (cashCard == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(cashCard);
    }

    @PostMapping
    private ResponseEntity<Void> createCashCard(@RequestBody CashCard newCashCardRequest, UriComponentsBuilder ucb,
                                                Principal principal) {
        CashCard cashCardWithOwner = new CashCard(null, newCashCardRequest.amount(), principal.getName());
        CashCard saved = cashCardRepository.save(cashCardWithOwner);
        URI location = ucb.path("/cashcards/{id}").buildAndExpand(saved.id()).toUri();
        return ResponseEntity.created(location).build();
    }

    @GetMapping
    private ResponseEntity<List<CashCard>> findAll(Pageable pageable, Principal principal) {
        Page<CashCard> page = cashCardRepository.findByOwner(principal.getName(),
                PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        pageable.getSortOr(Sort.by(Sort.Direction.ASC, "amount"))
                ));
        return ResponseEntity.ok(page.getContent());
    }

    @PutMapping("/{requestedId}")
    private ResponseEntity<Void> putCashCard(@PathVariable Long requestedId, @RequestBody CashCard cashCardUpdate,
                                             Principal principal) {
        CashCard cashCard = findCashCard(requestedId, principal);
        if (cashCard == null) {
            return ResponseEntity.notFound().build();
        }

        CashCard updatedCashCard = new CashCard(cashCard.id(), cashCardUpdate.amount(), cashCard.owner());
        cashCardRepository.save(updatedCashCard);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{requestedId}")
    public ResponseEntity<Void> deleteCashCard(@PathVariable Long requestedId, Principal principal) {
        if (cashCardRepository.existsByIdAndOwner(requestedId, principal.getName())) {
            cashCardRepository.deleteById(requestedId);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    private CashCard findCashCard(Long requestedId, Principal principal) {
        return cashCardRepository.findByIdAndOwner(requestedId, principal.getName());
    }
}
