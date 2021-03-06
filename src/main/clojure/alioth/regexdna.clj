;   Copyright (c) Rich Hickey and contributors.
;   All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;
;   Alioth benchmarks:
;   http://shootout.alioth.debian.org/u64q/benchmark.php?test=regexdna

(ns alioth.regexdna
  (:import [java.io FileDescriptor FileInputStream])
  (:import [java.nio ByteBuffer])
  (:import [java.nio.channels FileChannel])
  (:import [java.util.regex Pattern])
  (:gen-class))

(set! *warn-on-reflection* true)
(set! *unchecked-math* true)

(def ^:const comment-pattern ">.*\n|\n")
(def ^:const replace-pattern (Pattern/compile "[BDHKMNRSVWY]"))

(def replacements {"B" "(c|g|t)"
                   "D" "(a|g|t)"
                   "H" "(a|c|t)"
                   "K" "(g|t)"
                   "M" "(a|c)"
                   "N" "(a|c|g|t)"
                   "R" "(a|g)"
                   "S", "(c|g)"
                   "V", "(a|c|g)"
                   "W", "(a|t)"
                   "Y", "(c|t)"})

(def variants ["agggtaaa|tttaccct"
               "[cgt]gggtaaa|tttaccc[acg]"
               "a[act]ggtaaa|tttacc[agt]t"
               "ag[act]gtaaa|tttac[agt]ct"
               "agg[act]taaa|ttta[agt]cct"
               "aggg[acg]aaa|ttt[cgt]ccct"
               "agggt[cgt]aa|tt[acg]accct"
               "agggta[cgt]a|t[acg]taccct"
               "agggtaa[cgt]|[acg]ttaccct"
              ])

(defn count-occurrences [content regex]
  [regex (-> regex Pattern/compile (re-seq content) count)])

(defn replace-codes ^CharSequence [^String content]
  (let [buf (StringBuffer.)
        m (.matcher replace-pattern content)]
    (loop []
      (when (.find m)
        (.appendReplacement m buf "")
        (.append buf (get replacements (.group m)))
        (recur)))
    (.appendTail m buf)
    buf))

(defn regexdna [^bytes ba]
  (let [input (String. ba "US-ASCII")
        content (.replaceAll input comment-pattern "")
        replaced (future (replace-codes content))]
    (doseq [[regex match-count] (pmap #(count-occurrences content %) variants)]
      (println regex match-count))
    (println)
    (doseq [c (map count [input content @replaced])]
      (println c))))

(defn -main [& args]
  (let [cin (-> FileDescriptor/in FileInputStream. .getChannel)
        bb (-> cin .size int ByteBuffer/allocate)]
    (.read cin bb)
    (regexdna (.array bb)))
  (shutdown-agents))