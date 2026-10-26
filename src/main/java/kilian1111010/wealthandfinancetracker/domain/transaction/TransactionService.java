package kilian1111010.wealthandfinancetracker.domain.transaction;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import kilian1111010.wealthandfinancetracker.domain.PageResponse;
import kilian1111010.wealthandfinancetracker.domain.account.AccountEntity;
import kilian1111010.wealthandfinancetracker.domain.account.AccountService;
import kilian1111010.wealthandfinancetracker.domain.category.CategoryEntity;
import kilian1111010.wealthandfinancetracker.domain.category.CategoryService;
import kilian1111010.wealthandfinancetracker.exception.exceptions.InvalidRequestException;
import kilian1111010.wealthandfinancetracker.exception.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class TransactionService {

    private static final int MAX_PAGE_SIZE = 200;

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final CategoryService categoryService;

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> list(UUID userId, TransactionFilter filter, Pageable pageable) {
        PageRequest page = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "date").and(Sort.by(Sort.Direction.DESC, "id")));
        return PageResponse.of(this.transactionRepository.findAll(specification(userId, filter), page),
                TransactionResponse::of);
    }

    public TransactionResponse create(UUID userId, TransactionRequest request) {
        requireNonZero(request.amount());
        AccountEntity account = this.accountService.getForBooking(userId, request.accountId());
        TransactionEntity transaction = new TransactionEntity();
        apply(userId, transaction, account, request);
        this.accountService.changeBalance(account, request.amount());
        return TransactionResponse.of(this.transactionRepository.save(transaction));
    }

    public TransactionResponse update(UUID userId, UUID id, TransactionRequest request) {
        requireNonZero(request.amount());
        TransactionEntity transaction = getChangeable(userId, id);
        this.accountService.changeBalance(transaction.getAccount(), transaction.getAmount().negate());
        AccountEntity account = this.accountService.getForBooking(userId, request.accountId());
        apply(userId, transaction, account, request);
        this.accountService.changeBalance(account, request.amount());
        return TransactionResponse.of(transaction);
    }

    public void delete(UUID userId, UUID id) {
        TransactionEntity transaction = getChangeable(userId, id);
        this.accountService.changeBalance(transaction.getAccount(), transaction.getAmount().negate());
        this.transactionRepository.delete(transaction);
    }

    private TransactionEntity getChangeable(UUID userId, UUID id) {
        TransactionEntity transaction = this.transactionRepository.findByIdAndAccountUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Buchung nicht gefunden."));
        if (transaction.getAccount().isArchived()) {
            throw new InvalidRequestException("Buchungen archivierter Konten können nicht geändert werden.");
        }
        return transaction;
    }

    private void apply(UUID userId, TransactionEntity transaction, AccountEntity account, TransactionRequest request) {
        CategoryEntity category = request.categoryId() == null
                ? null
                : this.categoryService.get(userId, request.categoryId());
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setAmount(request.amount());
        transaction.setDate(request.date());
        transaction.setCounterparty(blankToNull(request.counterparty()));
        transaction.setDescription(blankToNull(request.description()));
    }

    private static Specification<TransactionEntity> specification(UUID userId, TransactionFilter filter) {
        return (root, query, builder) -> {
            Join<TransactionEntity, AccountEntity> account = root.join("account");
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(account.get("user").get("id"), userId));
            if (filter.from() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.<LocalDateTime>get("date"), filter.from().atStartOfDay()));
            }
            if (filter.to() != null) {
                predicates.add(builder.lessThan(root.<LocalDateTime>get("date"), filter.to().plusDays(1).atStartOfDay()));
            }
            if (filter.accountId() != null) {
                predicates.add(builder.equal(account.get("id"), filter.accountId()));
            }
            if (filter.categoryId() != null) {
                predicates.add(builder.equal(root.get("category").get("id"), filter.categoryId()));
            }
            if (filter.kind() == TransactionFilter.TransactionKind.INCOME) {
                predicates.add(builder.greaterThan(root.<BigDecimal>get("amount"), BigDecimal.ZERO));
            } else if (filter.kind() == TransactionFilter.TransactionKind.EXPENSE) {
                predicates.add(builder.lessThan(root.<BigDecimal>get("amount"), BigDecimal.ZERO));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static void requireNonZero(BigDecimal amount) {
        if (amount.signum() == 0) {
            throw new InvalidRequestException("Der Betrag darf nicht 0 sein.");
        }
    }

    private static @Nullable String blankToNull(@Nullable String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
