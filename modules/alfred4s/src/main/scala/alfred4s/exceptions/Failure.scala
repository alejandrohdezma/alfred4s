/*
 * Copyright 2024 Alejandro Hernández <https://github.com/alejandrohdezma>
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

package alfred4s.exceptions

import scala.util.control.NoStackTrace

/** This exception can be thrown using `alfred4s.fail` to cntrol your workflow's flow and fail fast. It will be captured
  * by `alfred4s.app` and appropiately returned to the user as an "error" item.
  */
final case class Failure(title: String, subtitle: String = "")
    extends RuntimeException(s"$title. $subtitle")
    with NoStackTrace
