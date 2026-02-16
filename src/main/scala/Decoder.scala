import Types.{Bit, Digit, Even, Odd, NoParity, One, Parity, Pixel, Str, Zero}
import scala.collection.immutable

object Decoder {
  // TODO 1.1
  def toBit(s: Char): Bit = if (s == '0') Zero else One
  def toBit(s: Int): Bit = if (s == 0) Zero else One

  // TODO 1.2
  def complement(c: Bit): Bit = if(c == Zero) One else Zero

  // TODO 1.3
  val LStrings: List[String] = List("0001101", "0011001", "0010011", "0111101", "0100011",
    "0110001", "0101111", "0111011", "0110111", "0001011")
  // codificări L
  val leftOddList: List[List[Bit]] = LStrings.map(l => l.map(toBit).toList)
  // codificări R
  val rightList: List[List[Bit]] = leftOddList.map(r => r.map(complement))
  // codificări G
  val leftEvenList: List[List[Bit]] = rightList.map(g => g.reverse)
  
  // TODO 1.4
  def group[A](l: List[A]): List[List[A]] = l match {
    case Nil => Nil
    case h :: t =>
      def aux(acc: List[A], rest: List[A]): (List[A], List[A]) = rest match {
        case x :: xs if x == h => aux(x :: acc, xs)
        case _ => (acc, rest)
      }
      val (groups, list) = aux(List(h), t)
      groups :: group(list)
  }
  
  // TODO 1.5
  def runLength[A](l: List[A]): List[(Int, A)] = group(l).map(g => (g.length, g.head))
  
  case class RatioInt(n: Int, d: Int) extends Ordered[RatioInt] {
    require(d != 0, "Denominator cannot be zero")
    private val gcd = BigInt(n).gcd(BigInt(d)).toInt
    val a = n / gcd // numărător
    val b = d / gcd // numitor

    override def toString: String = s"$a/$b"

    override def equals(obj: Any): Boolean = obj match {
      case that: RatioInt => this.a.abs == that.a.abs &&
        this.b.abs == that.b.abs &&
        this.a.sign * this.b.sign == that.a.sign * that.b.sign
      case _ => false
    }

    // TODO 2.1
    def -(other: RatioInt): RatioInt = RatioInt(a * other.b - other.a * b, b * other.b)
    def +(other: RatioInt): RatioInt = RatioInt(a * other.b + other.a * b, b * other.b)
    def *(other: RatioInt): RatioInt = RatioInt(a * other.a, b * other.b)
    def /(other: RatioInt): RatioInt = RatioInt(a * other.b, b * other.a)

    // TODO 2.2
    def compare(other: RatioInt): Int = (this - other).a
  }
  
  // TODO 3.1
  def scaleToOne[A](l: List[(Int, A)]): List[(RatioInt, A)] = {
    val total = l.map(_._1).sum
    l.map {
    s => val (cnt, e) = s
    (RatioInt(cnt, total), e)
    }
  }
  // TODO 3.2
  def scaledRunLength(l: List[(Int, Bit)]): (Bit, List[RatioInt]) = {
    val bits = scaleToOne(l).map(_._2)
    val ratios = scaleToOne(l).map(_._1)
    (bits.head, ratios)
  }
  
  // TODO 3.3
  def toParities(s: Str): List[Parity] = s.map {
    case 'L' => Odd
    case 'G' => Even
  }
  
  // TODO 3.4
  val PStrings: List[String] = List("LLLLLL", "LLGLGG", "LLGGLG", "LLGGGL", "LGLLGG",
    "LGGLLG", "LGGGLL", "LGLGLG", "LGLGGL", "LGGLGL")
  val leftParityList: List[List[Parity]] = PStrings.map(s => toParities(s.toList))

  // TODO 3.5
  type SRL = (Bit, List[RatioInt])
  val leftOddSRL: List[SRL] = leftOddList.map(runLength).map(scaledRunLength)
  val leftEvenSRL: List[SRL] = leftEvenList.map(runLength).map(scaledRunLength)
  val rightSRL: List[SRL] = rightList.map(runLength).map(scaledRunLength)

  // TODO 4.1
  def distance(l1: SRL, l2: SRL): RatioInt = {
  if (l1._1 != l2._1) {
    RatioInt(100, 1)
  } else {
    val diffs = (l1._2 zip l2._2).map {
      case (r1, r2) =>
      if(r1 > r2) r1 - r2 else r2 - r1
    }
     diffs.reduce(_ + _)
  }
}
  // TODO 4.2
  def bestMatch(SRL_Codes: List[SRL], digitCode: SRL): (RatioInt, Digit) = {
    SRL_Codes.zipWithIndex.map { case (code, index) =>
        (distance(code, digitCode), index) }.min
  }

  // TODO 4.3
  def bestLeft(digitCode: SRL): (Parity, Digit) = {
    val (oddDistance, oddDigit) = bestMatch(leftOddSRL, digitCode)
    val (evenDistance, evenDigit) = bestMatch(leftEvenSRL, digitCode)
    if (oddDistance < evenDistance) (Odd, oddDigit) else (Even, evenDigit)
  }

  // TODO 4.4
  def bestRight(digitCode: SRL): (Parity, Digit) = {
    val (distance, digit) = bestMatch(rightSRL, digitCode)
    (NoParity, digit)
  }

  def chunkWith[A](f: List[A] => (List[A], List[A]))(l: List[A]): List[List[A]] = {
    l match {
      case Nil => Nil
      case _ =>
        val (h, t) = f(l)
        h :: chunkWith(f)(t)
    }
  }
  
  def chunksOf[A](n: Int)(l: List[A]): List[List[A]] =
    chunkWith((l: List[A]) => l.splitAt(n))(l)

  // TODO 4.5
  def findLast12Digits(rle: List[(Int, Bit)]): List[(Parity, Digit)] = {
    require(rle.length == 59)
    val leftGroup = rle.drop(3).take(24)
    val rightGroup = rle.drop(32).take(24)
    val leftDigits = chunksOf(4)(leftGroup).map(scaledRunLength).map(bestLeft)
    val rightDigits = chunksOf(4)(rightGroup).map(scaledRunLength).map(bestRight)
    leftDigits ++ rightDigits
  }

  // TODO 4.6
  def firstDigit(l: List[(Parity, Digit)]): Option[Digit] = {
    val parities = l.take(6).map(_._1)
    leftParityList.zipWithIndex.find(_._1 == parities).map(_._2)
  }

  // TODO 4.7
  def checkDigit(l: List[Digit]): Digit = {
  val weights = l.zipWithIndex.map { case (_, index) => if (index % 2 == 0) 1 else 3 }
    val sum = (l zip weights).map { case (digit, weight) => digit * weight }.sum
  (10 - (sum % 10)) % 10
  }
  
  // TODO 4.8
  def verifyCode(code: List[(Parity, Digit)]): Option[String] = {
  firstDigit(code) match {
    case Some(first) =>
      val digits = first :: code.map(_._2)
      if (digits.length != 13) Some("error")
      else {
        val expected = checkDigit(digits.take(12))
        val actual = digits.drop(12).head
        if (expected == actual) Some(digits.mkString) else Some("error")
      }
    case None => Some(code.map(_._2).mkString)
  }
}

  // TODO 4.9
  def solve(rle: List[(Int, Bit)]): Option[String] = {
    val digits = findLast12Digits(rle)
    verifyCode(digits) match {
      case Some(errorMessage) => Some(errorMessage)
      case None =>
        firstDigit(digits) match {
          case Some(firstDigit) =>
            val completeCode = firstDigit :: digits.map(_._2)
            Some(completeCode.mkString)
          case None => Some("error")
        }
    }
  }

  def checkRow(row: List[Pixel]): List[List[(Int, Bit)]] = {
    val rle = runLength(row);

    def condition(sl: List[(Int, Pixel)]): Boolean = {
      if (sl.isEmpty) false
      else if (sl.size < 59) false
      else sl.head._2 == 1 &&
        sl.head._1 == sl.drop(2).head._1 &&
        sl.drop(56).head._1 == sl.drop(58).head._1
    }

    rle.sliding(59, 1)
      .filter(condition)
      .toList
      .map(_.map(pair => (pair._1, toBit(pair._2))))
  }
}


