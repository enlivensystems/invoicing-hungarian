package systems.enliven.invoicing.hungarian

import org.apache.commons.lang3.RandomStringUtils
import systems.enliven.invoicing.hungarian.api.data.{Address, TaxNumber}
import systems.enliven.invoicing.hungarian.api.recipient.{
  DomesticGroupVATSubject,
  DomesticTaxablePerson,
  EUTaxablePerson,
  NonEUTaxablePerson,
  NonTaxableNonPrivatePerson,
  PrivatePerson,
  Recipient
}

object TestDataGenerator {
  val faker = new com.github.javafaker.Faker()

  private def testName: String = faker.name().fullName()

  private def testTaxPayerID: String = RandomStringUtils.randomNumeric(8)
  private def testCountyCode: String = RandomStringUtils.randomNumeric(2)

  private def testBankAccountNumberA: String =
    RandomStringUtils.randomNumeric(8) + "-" +
      RandomStringUtils.randomNumeric(8) + "-" +
      RandomStringUtils.randomNumeric(8)

  private def testBankAccountNumberB: String =
    RandomStringUtils.randomNumeric(8) + "-" + RandomStringUtils.randomNumeric(8)

  private def testBankAccountNumberC: String =
    RandomStringUtils.randomAlphabetic(2).toUpperCase +
      RandomStringUtils.randomNumeric(2) +
      RandomStringUtils.randomAlphanumeric(11, 30)

  private def testBankAccountNumbers: Seq[Option[String]] =
    Seq(
      None,
      Some(testBankAccountNumberA),
      Some(testBankAccountNumberB),
      Some(testBankAccountNumberC)
    )

  private def testTaxNumbers(validVatCodes: Seq[Int] = 1 to 5): Seq[TaxNumber] = {
    require(validVatCodes.nonEmpty)
    def validVatCode: String = validVatCodes(scala.util.Random.nextInt(validVatCodes.size)).toString
    Seq(
      TaxNumber(testTaxPayerID, None, None),
      TaxNumber(testTaxPayerID, Some(validVatCode), None),
      TaxNumber(testTaxPayerID, None, Some(testCountyCode)),
      TaxNumber(testTaxPayerID, Some(validVatCode), Some(testCountyCode))
    )
  }

  def testAddress: Address =
    Address(
      countryCode = RandomStringUtils.randomAlphabetic(2).toUpperCase,
      region = None,
      postalCode = RandomStringUtils.randomAlphanumeric(4, 10).toUpperCase,
      city = faker.address().city(),
      streetName = faker.address().streetName(),
      publicPlaceCategory = faker.address().streetSuffix
    )

  def privatePersonTestCases: Seq[PrivatePerson] =
    testBankAccountNumbers.map(
      bankAccountNumber => PrivatePerson(bankAccountNumber = bankAccountNumber)
    )

  def domesticTaxablePersonTestCases: Seq[DomesticTaxablePerson] =
    for (bankAccountNumber <- testBankAccountNumbers;
      taxNumber <- testTaxNumbers()) yield {
      DomesticTaxablePerson(
        taxNumber = taxNumber,
        name = testName,
        address = testAddress,
        bankAccountNumber = bankAccountNumber
      )
    }

  def domesticGroupVATSubjectTestCases: Seq[DomesticGroupVATSubject] =
    for (bankAccountNumber <- testBankAccountNumbers;
      groupTaxNumber <- testTaxNumbers(Seq(5));
      memberTaxNumber <- testTaxNumbers(Seq(4))) yield {
      DomesticGroupVATSubject(
        groupTaxNumber = groupTaxNumber,
        memberTaxNumber = memberTaxNumber,
        name = testName,
        address = testAddress,
        bankAccountNumber = bankAccountNumber
      )
    }

  def euTaxablePersonTestCases: Seq[EUTaxablePerson] =
    for (bankAccountNumber <- testBankAccountNumbers) yield {
      EUTaxablePerson(
        communityVatNumber =
          RandomStringUtils.randomAlphabetic(2).toUpperCase +
            RandomStringUtils.randomAlphanumeric(2, 13).toUpperCase,
        name = testName,
        address = testAddress,
        bankAccountNumber = bankAccountNumber
      )
    }

  def nonEUTaxablePerson: Seq[NonEUTaxablePerson] =
    for (bankAccountNumber <- testBankAccountNumbers) yield {
      NonEUTaxablePerson(
        thirdStateTaxId = RandomStringUtils.randomAlphanumeric(10, 20).toUpperCase,
        name = testName,
        address = testAddress,
        bankAccountNumber = bankAccountNumber
      )
    }

  def nonTaxableNonPrivatePerson: Seq[NonTaxableNonPrivatePerson] =
    for (bankAccountNumber <- testBankAccountNumbers) yield {
      NonTaxableNonPrivatePerson(
        name = testName,
        address = testAddress,
        bankAccountNumber = bankAccountNumber
      )
    }

  def testRecipients: Seq[Recipient] =
    privatePersonTestCases.map(_.asInstanceOf[Recipient]) ++
      domesticTaxablePersonTestCases.map(_.asInstanceOf[Recipient]) ++
      domesticGroupVATSubjectTestCases.map(_.asInstanceOf[Recipient]) ++
      euTaxablePersonTestCases.map(_.asInstanceOf[Recipient]) ++
      nonEUTaxablePerson.map(_.asInstanceOf[Recipient]) ++
      nonTaxableNonPrivatePerson.map(_.asInstanceOf[Recipient])

}
