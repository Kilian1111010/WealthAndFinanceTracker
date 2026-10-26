package kilian1111010.wealthandfinancetracker.domain.transfer;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import kilian1111010.wealthandfinancetracker.domain.PageResponse;
import kilian1111010.wealthandfinancetracker.domain.account.AccountEntity;
import kilian1111010.wealthandfinancetracker.domain.account.AccountService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class TransferService {

    private static final int MAX_PAGE_SIZE = 200;

    private final TransferRepository transferRepository;
    private final AccountService accountService;

    @Transactional(readOnly = true)
    public PageResponse<TransferResponse> list(UUID userId, @Nullable LocalDate from, @Nullable LocalDate to,
                                               @Nullable UUID accountId, Pageable pageable) {
        PageRequest page = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "date").and(Sort.by(Sort.Direction.DESC, "id")));
        return PageResponse.of(this.transferRepository.findAll(specification(userId, from, to, accountId), page),
                TransferResponse::of);
    }

    public TransferResponse create(UUID userId, TransferRequest request) {
        TransferEntity transfer = new TransferEntity();
        book(userId, transfer, request);
        return TransferResponse.of(this.transferRepository.save(transfer));
    }

    public TransferResponse update(UUID userId, UUID id, TransferRequest request) {
        TransferEntity transfer = getChangeable(userId, id);
        revert(transfer);
        book(userId, transfer, request);
        return TransferResponse.of(transfer);
    }

    public void delete(UUID userId, UUID id) {
        TransferEntity transfer = getChangeable(userId, id);
        revert(transfer);
        this.transferRepository.delete(transfer);
    }

    private void book(UUID userId, TransferEntity transfer, TransferRequest request) {
        if (request.sendingAccountId().equals(request.receivingAccountId())) {
            throw new InvalidRequestException("Sender- und Empfängerkonto müssen verschieden sein.");
        }
        AccountEntity sending = this.accountService.getForBooking(userId, request.sendingAccountId());
        AccountEntity receiving = this.accountService.getForBooking(userId, request.receivingAccountId());
        BigDecimal receivedAmount = receivedAmount(sending, receiving, request);

        transfer.setSendingAccount(sending);
        transfer.setReceivingAccount(receiving);
        transfer.setSentAmount(request.sentAmount());
        transfer.setReceivedAmount(receivedAmount);
        transfer.setDate(request.date());
        transfer.setDescription(request.description() == null || request.description().isBlank()
                ? null
                : request.description().trim());

        this.accountService.changeBalance(sending, request.sentAmount().negate());
        this.accountService.changeBalance(receiving, receivedAmount);
    }

    private void revert(TransferEntity transfer) {
        this.accountService.changeBalance(transfer.getSendingAccount(), transfer.getSentAmount());
        this.accountService.changeBalance(transfer.getReceivingAccount(), transfer.getReceivedAmount().negate());
    }

    private static BigDecimal receivedAmount(AccountEntity sending, AccountEntity receiving, TransferRequest request) {
        if (Objects.equals(sending.getCurrency(), receiving.getCurrency())) {
            return request.receivedAmount() == null ? request.sentAmount() : request.receivedAmount();
        }
        if (request.receivedAmount() == null) {
            throw new InvalidRequestException(
                    "Bei unterschiedlichen Währungen muss der empfangene Betrag angegeben werden.");
        }
        return request.receivedAmount();
    }

    private TransferEntity getChangeable(UUID userId, UUID id) {
        TransferEntity transfer = this.transferRepository.findByIdAndSendingAccountUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Transfer nicht gefunden."));
        if (transfer.getSendingAccount().isArchived() || transfer.getReceivingAccount().isArchived()) {
            throw new InvalidRequestException("Transfers archivierter Konten können nicht geändert werden.");
        }
        return transfer;
    }

    private static Specification<TransferEntity> specification(UUID userId, @Nullable LocalDate from,
                                                               @Nullable LocalDate to, @Nullable UUID accountId) {
        return (root, query, builder) -> {
            Join<TransferEntity, AccountEntity> sending = root.join("sendingAccount");
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(sending.get("user").get("id"), userId));
            if (from != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.<LocalDateTime>get("date"), from.atStartOfDay()));
            }
            if (to != null) {
                predicates.add(builder.lessThan(root.<LocalDateTime>get("date"), to.plusDays(1).atStartOfDay()));
            }
            if (accountId != null) {
                predicates.add(builder.or(
                        builder.equal(sending.get("id"), accountId),
                        builder.equal(root.get("receivingAccount").get("id"), accountId)));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
