package co.uproot.abandon

import org.scalatest.FlatSpec
import org.scalatest.matchers.Matcher
import org.scalatest.Matchers
import scala.util.parsing.input.PagedSeqReader
import scala.collection.immutable.PagedSeq
import org.scalatest.Inside

object TestHelper {
  def reader(s: String) = new PagedSeqReader(PagedSeq.fromStrings(collection.immutable.Seq(s)))
  def mkScanner(r: PagedSeqReader) = new AbandonParser.lexical.Scanner(r)
  def scanner(s: String) = mkScanner(reader(s))
  def nlit(n: BigDecimal) = NumericLiteralExpr(n)

  val expenseAccount = AccountName(Seq("Expense"))
  val cashAccount = AccountName(Seq("Cash"))
  val bankAccount = AccountName(Seq("Bank", "Current"))
}

import TestHelper._

class ProcessorTest extends FlatSpec with Matchers with Inside  {

  "Processor" should "export a simple transaction in ledger Format" in {
    val testInput = """
    2013/1/1
      Expense       -(200 + 40)
      Cash
    """
    val parseResult = AbandonParser.abandon(scanner(testInput))
    inside(parseResult) {
      case AbandonParser.Success(result, _) =>
          val astEntries = result
          val appState = Processor.process(astEntries)
           val exports = Seq(LedgerExportSettings(None,Seq("balSheet12.txt"),false))

           val settings = Settings(Nil, Nil, Nil, ReportOptions(Nil), exports, None)

           exports.foreach { exportSettings =>
              exportSettings match {
                case balSettings: LedgerExportSettings =>
                  val ledgerRep = Reports.ledgerExport(appState,settings,balSettings)
                  inside(ledgerRep) {
                    case List(LedgerExportData(date,txns)) =>
                      date should be(Date(2013, 1, 1))
                      inside(txns) {
                        case List(LedgerExportEntry(acc1, expr1), LedgerExportEntry(acc2, expr2)) =>
                        acc1 should be (cashAccount)
                        acc2 should be (expenseAccount)
                        expr1 should be (240)
                        expr2 should be (-240)
                      }
                  }
              }
           }
          }
    }

   "Processor" should "not export a transaction with zero value" in {
    val testInput = """
    2013/1/1
      Expense       -(200 + 40)
      Cash
      Bank           0000
    """
    val parseResult = AbandonParser.abandon(scanner(testInput))
    inside(parseResult) {
      case AbandonParser.Success(result, _) =>
          val astEntries = result
          val appState = Processor.process(astEntries)
           val exports = Seq(LedgerExportSettings(None,Seq("balSheet12.txt"),false))

           val settings = Settings(Nil, Nil, Nil, ReportOptions(Nil), exports, None)

           exports.foreach { exportSettings =>
              exportSettings match {
                case balSettings: LedgerExportSettings =>
                  val ledgerRep = Reports.ledgerExport(appState,settings,balSettings)
                  inside(ledgerRep) {
                    case List(LedgerExportData(date,txns)) =>
                      date should be(Date(2013, 1, 1))
                      inside(txns) {
                        case List(LedgerExportEntry(acc1, expr1), LedgerExportEntry(acc2, expr2)) =>
                        acc1 should be (cashAccount)
                        acc2 should be (expenseAccount)
                        expr1 should be (240)
                        expr2 should be (-240)
                      }
                  }
              }
           }
          }
    }

   "Processor" should "export no transaction" in {
    val testInput = """
    """
    val parseResult = AbandonParser.abandon(scanner(testInput))
    inside(parseResult) {
      case AbandonParser.Success(result, _) =>
          val astEntries = result
          val appState = Processor.process(astEntries)
           val exports = Seq(LedgerExportSettings(None,Seq("balSheet12.txt"),false))

           val settings = Settings(Nil, Nil, Nil, ReportOptions(Nil), exports, None)

           exports.foreach { exportSettings =>
              exportSettings match {
                case balSettings: LedgerExportSettings =>
                  val ledgerRep = Reports.ledgerExport(appState,settings,balSettings)
                  ledgerRep should be (List())
              }
           }
          }
    }
}