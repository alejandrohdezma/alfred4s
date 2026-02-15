/*
 * Copyright 2024-2025 Alejandro Hernández <https://github.com/alejandrohdezma>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package alfred4s.internal

import scala.annotation.tailrec

/** Copied from scala.sys.process.Parser */
@SuppressWarnings(Array("all"))
private[alfred4s] def tokenize(line: String, errorFn: String => Unit): List[String] = {
  val DQ = '"'

  val SQ = '\''

  val EOF = -1

  import Character.isWhitespace
  import java.lang.{StringBuilder => Builder}
  import collection.mutable.ArrayBuffer

  var accum: List[String] = Nil
  var pos                 = 0
  var start               = 0
  val qpos                = new ArrayBuffer[Int](16) // positions of paired quotes

  def cur: Int = if (done) EOF else line.charAt(pos)
  def bump()   = pos += 1
  def done     = pos >= line.length

  // Skip to the next quote as given.
  def skipToQuote(q: Int): Boolean = {
    var escaped           = false
    def terminal: Boolean = cur match {
      case _ if escaped => escaped = false; false
      case '\\'         => escaped = true; false
      case `q` | EOF    => true
      case _            => false
    }
    while (!terminal) bump()
    !done
  }
  // Skip to a word boundary, where words can be quoted and quotes can be escaped
  def skipToDelim(): Boolean = {
    var escaped                     = false
    def quote()                     = { qpos += pos; bump() }
    @tailrec def advance(): Boolean = cur match {
      case _ if escaped         => escaped = false; bump(); advance()
      case '\\'                 => escaped = true; bump(); advance()
      case q @ (DQ | SQ)        => { quote(); skipToQuote(q) } && { quote(); advance() }
      case EOF                  => true
      case c if isWhitespace(c) => true
      case _                    => bump(); advance()
    }
    advance()
  }
  def skipWhitespace() = while (isWhitespace(cur)) bump()
  def copyText()       = {
    val buf = new Builder
    var p   = start
    var i   = 0
    while (p < pos) {
      if (i >= qpos.size) {
        buf.append(line, p, pos)
        p = pos
      } else if (p == qpos(i)) {
        buf.append(line, qpos(i) + 1, qpos(i + 1))
        p = qpos(i + 1) + 1
        i += 2
      } else {
        buf.append(line, p, qpos(i))
        p = qpos(i)
      }
    }
    buf.toString
  }
  def text() = {
    val res =
      if (qpos.isEmpty) line.substring(start, pos)
      else if (qpos(0) == start && qpos(1) == pos) line.substring(start + 1, pos - 1)
      else copyText()
    qpos.clear()
    res
  }
  def badquote() = errorFn(s"Unmatched quote [${qpos.last}](${line.charAt(qpos.last)})")

  @tailrec def loop(): List[String] = {
    skipWhitespace()
    start = pos
    if (done) accum.reverse
    else if (!skipToDelim()) { badquote(); Nil }
    else {
      accum ::= text()
      loop()
    }
  }
  loop()
}
