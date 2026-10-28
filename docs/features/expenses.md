# Group expenses

Group expenses let the members of a Group chat record shared costs, see who owes whom and settle
up. They are implemented as **Group message parts**, like [polls](polls.md): there is no separate
server or ledger. Expenses are encrypted and delivered through the normal Group message path.

Modules:

- `:feature:expenses` owns the expense screens, balance/settlement calculation, currency
  conversion and the composer handoff to Chats;
- `:feature:ocr` reads the total and merchant from a receipt photo (see
  [Receipt scanning](#receipt-scanning));
- `:feature:payment` prepares SEPA transfers and records settlement status (see
  [Settle up and payment](payment.md));
- `:feature:chats` (`GroupExpenseController`) connects the screens to the conversation and submits
  commands through `ConversationOrchestrator`;
- `:feature:conversationorchestration:groupstate` contains the expense workflows;
- shared models live in `:core:base` (`com.cbgm.sparrow.core.base.messagepart.domain.model`).

Expenses are Group-only. `OutgoingMessageContentPolicy` rejects polls and expenses in Direct chats
and requires them to be sent without a separate text part.

## How the user uses it

1. **Activate.** A group admin opens the attachment menu of the Group chat composer and, under
   *Group features*, chooses *Activate expenses* and picks the group currency (*Choose the group
   currency*). All balances are calculated in this currency.
2. **Pinned bar.** The active expense board occupies the group's pin slot. The pinned bar shows the
   number of expenses, the total and the local member's balance (*You owe*, *You're owed* or
   *Settled up*).
3. **Add expenses.** While the board is active, any member can choose *Add expense* in the same
   menu and enter a description, amount, currency, category, who paid and who shares it.
   Several expenses can be collected first and sent together (*Send expenses*).
4. **Overview.** The overview shows the local balance, group total, own share, the amount the
   local member paid, the balances of all members, the expense list and the *Settle up*
   suggestions.
5. **Edit or delete.** The detail screen of an expense allows editing and deleting. A deleted
   expense is removed for everyone in the group.
6. **Settle up.** A suggested transfer opens the payment screen (see [payment](payment.md)).
7. **Close.** A group admin can choose *Close expenses* in the attachment menu. This ends the
   board and frees the pin slot.

Activation, closing, new expenses, edits, deletions and payment status changes also appear as
activity entries in the chat history (for example "Anna added an expense").

## Model

The message parts in `core/base` are:

| Part | Content |
|---|---|
| `ExpenseBoard` | board `id`, group `currencyCode`, activation time, optional closing time |
| `ExpenseBoardClosed` | event that closes an existing board |
| `Expense` | description, `amountMinor`, `currencyCode`, `paidByMemberId`, `allocations`, `category`, optional `receipt` image, optional `conversion`, revision fields |
| `PaymentTransfer` | settlement status event (see [payment](payment.md)) |

Amounts are integers in minor units (for example cents). Members are identified by
`ExpenseMemberId.fromSigningPublicKey(...)`, the hex form of their **group signing key**, never by
local contact IDs.

`ExpensePolicy` validates every board and expense:

- description is not blank and at most 500 characters;
- amount is positive and the currency is an uppercase three-letter ISO 4217 code;
- 1–256 unique participants, every share is positive and the shares add up exactly to the
  amount;
- a conversion has a different original currency, a positive original amount and a valid rate;
- a receipt is a valid image attachment within `MessageAttachmentPolicy.MAX_IMAGE_BYTES`.

Categories (`ExpenseCategory`): flight, food, activity, parking, accommodation, transport,
shopping, other.

## Splitting

The split screen offers two modes:

- **Equal split** — `ExpenseSplit.evenly(...)` divides the amount between the selected members.
  The remainder is distributed one minor unit at a time in stable member-ID order, so the shares
  always add up to the full amount.
- **Custom split** — the user enters an amount per member. The screen shows how much is still
  left to assign or assigned too much; the expense can only be sent when everything is assigned.

## Currencies and exchange rates

An expense can be entered in a currency other than the group currency. The app then converts it
and stores the original amount, original currency and rate in `ExpenseConversion`, so every member
sees the same converted amount ("Paid … at …").

Exchange rates are loaded by `RemoteExchangeRateDataSource` from the connected Community Node
(`GET /v1/exchange-rates`) and cached locally (`LocalExchangeRateDataSource`).
`ExchangeRateRepositoryImpl.refreshIfStale()` asks for new rates at most once every 24 hours; without
a node connection the cached rates stay in use. The node fetches the euro reference rates from the
Frankfurter API (`https://api.frankfurter.dev/v2/rates?base=EUR`), caches them for 24 hours for all
clients and passes them through unchanged, so the rate provider sees the node and never the user's
address (see [`:server:exchange-rates`](../server/community-node.md#exchange-rates)). The request
contains no expense or user data. Without a rate for a currency the screen shows *No exchange rate
available*.

## Revisions: edit and delete

Expenses are never changed in place. An edit or delete sends a new `Expense` part whose
`revisionOf` points to the original ID and which carries `editedAtEpochMilliseconds` or
`deletedAtEpochMilliseconds`. `ExpenseRevisions.current(...)` picks the latest revision per
expense and drops deleted ones.

Only the creator of an expense or a group admin may edit or delete it. Incoming expenses are
checked by `AuthorizeIncomingExpensesUseCase` / `IncomingExpensePolicy`:

- the sender must be a current group member;
- a new expense must name its sender as creator, unless the sender is an admin;
- a revision must reference a known original, keep its creator, and come from the creator or an
  admin;
- expenses for a closed board are rejected.

Expense and payment parts cannot be edited like ordinary messages (`MessageEditPolicy`).

## Balances and settlement suggestions

`ObserveExpenseOverviewUseCase` builds the overview from the current revisions:

- `CalculateExpenseBalancesUseCase` credits the payer and debits every participant by their share.
  Only **confirmed** payments (a `SENT` event with a matching `RECEIVED` event, see
  `ExpensePayments.confirmed(...)`) change the balances.
- `CalculateExpenseSettlementsUseCase` checks that the balances add up to zero and suggests
  transfers by matching debtors with creditors (sorted by member ID). No money moves here; the
  UI states that payments happen outside Sparrow.
- `ObserveExpenseSummaryUseCase` provides the count, total and local balance for the pinned bar.

## Workflows

| Command | Workflow | Steps |
|---|---|---|
| `GroupMessageCommand.ActivateExpenses` | `ActivateGroupExpensesWorkflow` | AuthorizeAdmin → CheckNoActiveBoard → CreateBoard → SendBoard → PinBoard |
| `GroupMessageCommand.CreateExpense` | `CreateGroupExpenseWorkflow` | FindExpenseMembers → CreateExpense → SendExpense |
| `GroupMessageCommand.EditExpense` | `EditGroupExpenseWorkflow` | FindExpenseMembers → ReviseExpense → SendExpense → DeleteReplacedReceipts |
| `GroupMessageCommand.DeleteExpense` | `DeleteGroupExpenseWorkflow` | FindExpenseMembers → MarkExpenseDeleted → SendExpense → DeleteReceipts |
| `GroupMessageCommand.CloseExpenses` | `CloseGroupExpensesWorkflow` | AuthorizeAdmin → FindPin → FindBoardClosure → [SendBoardClosedMessage] → DeleteReceipts → PrepareEmptyPin → CreatePinPackets → EnqueuePinPackets → SaveEmptyPin |
| `GroupStateCommand.SendCurrentGroupExpenses` | `SendCurrentGroupExpensesWorkflow` | FindBoard → LoadExpenseMessages → EnqueueExpenseMessages |

Activations of one group run one after another under the `group-expense:<groupId>` lock, so a double tap
opens one board: the second activation finds the board the first one pinned in CheckNoActiveBoard and fails with
"Group expenses are already active".

Closing is safe to repeat after a failure: the closing message is not sent again when a stored
`ExpenseBoardClosed` part for the board already exists, receipts are deleted idempotently, and the
pin is cleared locally only after its packets are queued, so the retry still releases it for every
member.

While a board is active, `PinGroupMessageWorkflow` refuses to replace it with another pin.
`SendCurrentGroupExpensesWorkflow` is started from `SendCurrentGroupMetadataWorkflow`, so a member
who joins later receives the current board and its expenses.

## Composer handoff

The expense screens do not send group messages themselves. `CreateExpenseViewModel`,
`ExpenseOverviewViewModel` and `ExpenseDetailViewModel` submit an `ExpenseRequest` (`Create`,
`Edit`, `Delete`) through `SubmitExpenseRequestUseCase` to the `ExpenseComposerRepository`.
`GroupExpenseController` observes these requests for its group, maps them to
`GroupMessageCommand`s and runs them through the orchestrator. In the other direction it publishes
the group's `ExpenseGroup` (board, members, expenses, transfers) with `PublishExpenseGroupUseCase`
and the derived payment context for `:feature:payment`.

## Receipt scanning

The expense form has a receipt row (*Receipt* / *Auto import*). Tapping it opens the camera
document scanner (`rememberDocumentScanLauncher` in `:feature:media`). The scanned image is
attached to the expense as an encrypted image attachment, and `ScanReceiptUseCase` reads it:

1. `TextRecognizer` recognizes the text lines with their positions on the device: Google ML Kit
   text recognition (`com.google.mlkit:text-recognition`, `AndroidTextRecognizer`) on Android and
   Apple Vision (`IosTextRecognizer`) on iOS. The image is not sent to an OCR service.
2. `ReceiptTextParser` groups the lines into rows and looks for the total: an amount that equals
   the sum of the aligned item amounts above it, otherwise the most frequent of the tallest
   amounts. The merchant is the most prominent name-like row above the first amount.
3. The result (`ReceiptScan`: `totalMinor`, `merchant`) fills the amount and description, but
   only if the user has not typed their own values.

The scan can be repeated (*Receipt attached – tap to scan again*) or the receipt removed. Unsent
receipt files are deleted when the form is closed; receipts of replaced or deleted expenses and of
a closed board are deleted as well.

## Platform notes

The feature is used on Android. The iOS source sets contain text recognition and secure storage
implementations, but the iOS app as a whole is not runnable yet.
