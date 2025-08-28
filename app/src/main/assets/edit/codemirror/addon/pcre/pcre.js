// Declare global variables to avoid warnings in JSHint
/* global CodeMirror, define */

(function (mod) {
	if (typeof exports === "object" && typeof module === "object") // CommonJS
		mod(require("codemirror/lib/codemirror"));
	else if (typeof define === "function" && define.amd) // AMD
		define(["codemirror/lib/codemirror"], mod);
	else // Plain browser env
		mod(CodeMirror);
})(function (CodeMirror) {
	"use strict";

	CodeMirror.defineMode('pcre', function(editor_options, mode_options) {
		// Default settings:
		var options = {
			extended: true,
		};
		// Override default settings with user-provided settings:
		if ('extended' in mode_options) options.extended = Boolean(mode_options.extended);

		var delimiters = {
			'<': '>',
			'[': ']',
			'{': '}',
			'(': ')',
		};
		// Behaviour of alphanumeric characters after a backslash character (normal context):
		var backslash_in_normal_context = {
			'0': 'non-printing-character',
			'1': 'backreference',
			'2': 'backreference',
			'3': 'backreference',
			'4': 'backreference',
			'5': 'backreference',
			'6': 'backreference',
			'7': 'backreference',
			'8': 'backreference',
			'9': 'backreference',
			'A': 'anchor', // \A  start of subject
			'B': 'anchor', // \B  not a word boundary
			'C': 'generic-character-type', // \C  one data unit, even in UTF mode (best avoided)
			'D': 'generic-character-type', // \D  any character that is not a decimal digit
			'E': 'err no-error-message', // \E ends \Q but never matches 'E' -- PCRE does not emit any error message
			'F': '', // \F matches F
			'G': 'anchor', // \G  first matching position in subject
			'H': 'generic-character-type', // \H  any character that is not a horizontal white space character
			'I': '', // \I matches I
			'J': '', // \J matches J
			'K': 'anchor', // \K  reset start of match (neither an anchor nor a simple assertion)
			'L': 'err unsupported-escape-sequence', // PCRE does not support \L, \l, \N{name}, \U, or \u
			'M': '', // \M matches M
			'N': 'generic-character-type', // \N  a character that is not a newline
			'O': '', // \O matches O
			'P': 'err malformed-backslash-p-sequence', // malformed \P or \p sequence
			'Q': 'escaped-sequence-start', // \Q starts \Q...\E escape sequences.
			'R': 'generic-character-type', // \R  a newline sequence
			'S': 'generic-character-type', // \S  any character that is not a white space character
			'T': '', // \T matches T
			'U': 'unsupported-escape-sequence', // PCRE does not support \L, \l, \N{name}, \U, or \u
			'V': 'generic-character-type', // \V  any character that is not a vertical white space character
			'W': 'generic-character-type', // \W  any "non-word" character
			'X': 'generic-character-type', // \X  a Unicode extended grapheme cluster
			'Y': '', // \Y matches Y
			'Z': 'anchor', // \Z  matches at the end of the subject; also matches before a newline at the end of the subject
			'a': 'non-printing-character', // \a  alarm, that is, the BEL character (hex 07)
			'b': 'anchor', // \b  word boundary
			'c': 'err backslash-c-at-end-of-pattern', // \cx "control-x", where x is any ASCII character
			'd': 'generic-character-type', // \d  any decimal digi
			'e': 'non-printing-character', // \e  escape (hex 1B)
			'f': 'non-printing-character', // \f  form feed (hex 0C)
			'g': 'err a-number-reference-must-not-be-zero', // a numbered reference must not be zero
			'h': 'generic-character-type', // \h  any horizontal white space character
			'i': '', // \i matches i
			'j': '', // \j matches j
			'k': 'err backslash-k-is-not-followed-by-a-name', // \k is not followed by a [...] name
			'l': 'unsupported-espace-sequence', // PCRE does not support \L, \l, \N{name}, \U, or \u
			'm': '', // \m matches m
			'n': 'non-printing-character', // \n  linefeed (hex 0A)
			'o': '', // \o matches o
			'p': 'err malformed-backslash-p-sequence', // malformed \P or \p sequence
			'q': '', // \q matches q
			'r': 'non-printing-character', // \r  carriage return (hex 0D)
			's': 'generic-character-type', // \s  any white space character
			't': 'non-printing-character', // \t  tab (hex 09)
			'u': 'err unsupported-escape-sequence', // PCRE does not support \L, \l, \N{name}, \U, or \u
			'v': 'generic-character-type', // \v  any vertical white space character
			'w': 'generic-character-type', // any "word" character
			'x': 'non-printing-character', // binary zero (or x if PCRE_JAVASCRIPT_COMPAT)
			'y': '', // \y matches y
			'z': 'anchor', // \z  end of subject
		};
		// Behaviour of alphanumeric characters after a backslash character (character class context, i.e. [...]):
		var backslash_in_character_class = {
			'0': 'non-printing-character', // octal code
			'1': 'non-printing-character', // octal code
			'2': 'non-printing-character', // octal code
			'3': 'non-printing-character', // octal code
			'4': 'non-printing-character', // octal code
			'5': 'non-printing-character', // octal code
			'6': 'non-printing-character', // octal code
			'7': 'non-printing-character', // octal code
			'8': '', // \8 matches 8
			'9': '', // \9 matches 9
			'A': '', // \A matches A
			'B': '', // \B matches B -- \B, \R, and \X are not special inside a character class.
			'C': '', // \C matches C
			'D': 'generic-character-type', // \D  any character that is not a decimal digit
			'E': 'err no-error-message', // \E ends \Q but never matches 'E' -- PCRE does not emit any error message
			'F': '', // \F matches F
			'G': '', // \G matches G
			'H': 'generic-character-type', // \H  any character that is not a horizontal white space character
			'I': '', // \I matches I
			'J': '', // \J matches J
			'K': '', // \K matches K
			'L': 'err unsupported-escape-sequence', // PCRE does not support \L, \l, \N{name}, \U, or \u
			'M': '', // \M matches M
			'N': 'err backslash-n-is-not-supported-in-a-class', // \N is not allowed in a character class.
			'O': '', // \O matches O
			'P': 'err malformed-backslash-p-sequence', // malformed \P or \p sequence
			'Q': 'escaped-sequence-start', // \Q starts \Q...\E escape sequences.
			'R': '', // \R matches R -- \B, \R, and \X are not special inside a character class.
			'S': 'generic-character-type', // \S  any character that is not a white space character
			'T': '', // \T matches T
			'U': 'unsupported-escape-sequence', // PCRE does not support \L, \l, \N{name}, \U, or \u
			'V': 'generic-character-type', // \V  any character that is not a vertical white space character
			'W': 'generic-character-type', // \W  any "non-word" character
			'X': '', // \X matches X -- \B, \R, and \X are not special inside a character class.
			'Y': '', // \Y matches Y
			'Z': '', // \Z matches Z
			'a': 'non-printing-character', // \a  alarm, that is, the BEL character (hex 07)
			'b': 'non-printing-character', // inside a character class, \b is interpreted as the backspace character (hex 08)
			'c': 'err backslash-c-at-end-of-pattern', // \cx "control-x", where x is any ASCII character
			'd': 'generic-character-type', // \d  any decimal digi
			'e': 'non-printing-character', // \e  escape (hex 1B)
			'f': 'non-printing-character', // \f  form feed (hex 0C)
			'g': '', // \g matches g
			'h': 'generic-character-type', // \h  any horizontal white space character
			'i': '', // \i matches i
			'j': '', // \j matches j
			'k': '', // \k matches k
			'l': 'unsupported-espace-sequence', // PCRE does not support \L, \l, \N{name}, \U, or \u
			'm': '', // \m matches m
			'n': 'non-printing-character', // \n  linefeed (hex 0A)
			'o': '', // \o matches o
			'p': 'err malformed-backslash-p-sequence', // malformed \P or \p sequence
			'q': '', // \q matches q
			'r': 'non-printing-character', // \r  carriage return (hex 0D)
			's': 'generic-character-type', // \s  any white space character
			't': 'non-printing-character', // \t  tab (hex 09)
			'u': 'err unsupported-escape-sequence', // PCRE does not support \L, \l, \N{name}, \U, or \u
			'v': 'generic-character-type', // \v  any vertical white space character
			'w': 'generic-character-type', // any "word" character
			'x': 'non-printing-character', // binary zero (or x if PCRE_JAVASCRIPT_COMPAT)
			'y': '', // \y matches y
			'z': '', // \z matches z
		};
		var backslask_p_properties = {
			// GENERAL CATEGORY PROPERTIES FOR \p and \P
			'C': 'Other',
			'Cc': 'Control',
			'Cf': 'Format',
			'Cn': 'Unassigned',
			'Co': 'Private use',
			'Cs': 'Surrogate',

			'L': 'Letter',
			'Ll': 'Lower case letter',
			'Lm': 'Modifier letter',
			'Lo': 'Other letter',
			'Lt': 'Title case letter',
			'Lu': 'Upper case letter',
			'L&': 'Ll, Lu, or Lt',

			'M': 'Mark',
			'Mc': 'Spacing mark',
			'Me': 'Enclosing mark',
			'Mn': 'Non-spacing mark',

			'N': 'Number',
			'Nd': 'Decimal number',
			'Nl': 'Letter number',
			'No': 'Other number',

			'P': 'Punctuation',
			'Pc': 'Connector punctuation',
			'Pd': 'Dash punctuation',
			'Pe': 'Close punctuation',
			'Pf': 'Final punctuation',
			'Pi': 'Initial punctuation',
			'Po': 'Other punctuation',
			'Ps': 'Open punctuation',

			'S': 'Symbol',
			'Sc': 'Currency symbol',
			'Sk': 'Modifier symbol',
			'Sm': 'Mathematical symbol',
			'So': 'Other symbol',

			'Z': 'Separator',
			'Zl': 'Line separator',
			'Zp': 'Paragraph separator',
			'Zs': 'Space separator',

			// PCRE SPECIAL CATEGORY PROPERTIES FOR \p and \P
			'Xan': 'Alphanumeric: union of properties L and N',
			'Xps': 'POSIX space: property Z or tab, NL, VT, FF, CR',
			'Xsp': 'Perl space: property Z or tab, NL, VT, FF, CR',
			'Xuc': 'Univerally-named character: one that can be represented by a Universal Character Name',
			'Xwd': 'Perl word: property Xan or underscore',

			// SCRIPT NAMES FOR \p AND \P
			'Arabic': true,
			'Armenian': true,
			'Avestan': true,
			'Balinese': true,
			'Bamum': true,
			'Bassa_Vah': true,
			'Batak': true,
			'Bengali': true,
			'Bopomofo': true,
			'Brahmi': true,
			'Braille': true,
			'Buginese': true,
			'Buhid': true,
			'Canadian_Aboriginal': true,
			'Carian': true,
			'Caucasian_Albanian': true,
			'Chakma': true,
			'Cham': true,
			'Cherokee': true,
			'Common': true,
			'Coptic': true,
			'Cuneiform': true,
			'Cypriot': true,
			'Cyrillic': true,
			'Deseret': true,
			'Devanagari': true,
			'Duployan': true,
			'Egyptian_Hieroglyphs': true,
			'Elbasan': true,
			'Ethiopic': true,
			'Georgian': true,
			'Glagolitic': true,
			'Gothic': true,
			'Grantha': true,
			'Greek': true,
			'Gujarati': true,
			'Gurmukhi': true,
			'Han': true,
			'Hangul': true,
			'Hanunoo': true,
			'Hebrew': true,
			'Hiragana': true,
			'Imperial_Aramaic': true,
			'Inherited': true,
			'Inscriptional_Pahlavi': true,
			'Inscriptional_Parthian': true,
			'Javanese': true,
			'Kaithi': true,
			'Kannada': true,
			'Katakana': true,
			'Kayah_Li': true,
			'Kharoshthi': true,
			'Khmer': true,
			'Khojki': true,
			'Khudawadi': true,
			'Lao': true,
			'Latin': true,
			'Lepcha': true,
			'Limbu': true,
			'Linear_A': true,
			'Linear_B': true,
			'Lisu': true,
			'Lycian': true,
			'Lydian': true,
			'Mahajani': true,
			'Malayalam': true,
			'Mandaic': true,
			'Manichaean': true,
			'Meetei_Mayek': true,
			'Mende_Kikakui': true,
			'Meroitic_Cursive': true,
			'Meroitic_Hieroglyphs': true,
			'Miao': true,
			'Modi': true,
			'Mongolian': true,
			'Mro': true,
			'Myanmar': true,
			'Nabataean': true,
			'New_Tai_Lue': true,
			'Nko': true,
			'Ogham': true,
			'Ol_Chiki': true,
			'Old_Italic': true,
			'Old_North_Arabian': true,
			'Old_Permic': true,
			'Old_Persian': true,
			'Old_South_Arabian': true,
			'Old_Turkic': true,
			'Oriya': true,
			'Osmanya': true,
			'Pahawh_Hmong': true,
			'Palmyrene': true,
			'Pau_Cin_Hau': true,
			'Phags_Pa': true,
			'Phoenician': true,
			'Psalter_Pahlavi': true,
			'Rejang': true,
			'Runic': true,
			'Samaritan': true,
			'Saurashtra': true,
			'Sharada': true,
			'Shavian': true,
			'Siddham': true,
			'Sinhala': true,
			'Sora_Sompeng': true,
			'Sundanese': true,
			'Syloti_Nagri': true,
			'Syriac': true,
			'Tagalog': true,
			'Tagbanwa': true,
			'Tai_Le': true,
			'Tai_Tham': true,
			'Tai_Viet': true,
			'Takri': true,
			'Tamil': true,
			'Telugu': true,
			'Thaana': true,
			'Thai': true,
			'Tibetan': true,
			'Tifinagh': true,
			'Tirhuta': true,
			'Ugaritic': true,
			'Vai': true,
			'Warang_Citi': true,
			'Yi': true,
		};
		var backslash_p_regex_string = '[pP]\\{\\^?([\\w&]+)\\}';
		var backslash_p_regex = new RegExp(backslash_p_regex_string);

		var posix_named_sets = {
			'alnum': 'alphanumeric',
			'alpha': 'alphabetic',
			'ascii': '0-127',
			'blank': 'space or tab',
			'cntrl': 'control character',
			'digit': 'decimal digit',
			'graph': 'printing, excluding space',
			'lower': 'lower case letter',
			'print': 'printing, including space',
			'punct': 'printing, excluding alphanumeric',
			'space': 'white space',
			'upper': 'upper case letter',
			'word': 'same as \\w',
			'xdigit': 'hexadecimal digit',
		};
		// Include '<' and '>' to spot errors such as [a[:<:]b]
		var posix_named_sets_regex_string = '\\[:\\^?([\\w<>]+):]';
		var posix_named_sets_regex = new RegExp(posix_named_sets_regex_string);

		var callout_regex_string = '\\(\\?C(\\d{0,3})\\)';
		var callout_regex = new RegExp(callout_regex_string);

		var assertion_regex_string = '\\(\\?<?[=!]';
		var assertion_regex = new RegExp(assertion_regex_string);

		var condition_callout_regex_string = callout_regex_string + assertion_regex_string;
		var condition_callout_regex = new RegExp(condition_callout_regex_string);

		// (?i)     caseless
		// (?J)     allow duplicate names
		// (?m)     multiline
		// (?s)     single line (dotall)
		// (?U)     default ungreedy (lazy)
		// (?x)     extended (ignore white space)
		// (?-...)  unset option(s)
		// + combinations e.g. (?im-sx) or (?iJm-s-U-x)
		var options_regex_string = '(?:-?[iJmsUx]+)+';

		// Standalone option sequence, e.g. (?x-i)
		var option_sequence_regex_string = '\\(\\?' + options_regex_string + '\\)';
		var option_sequence_regex = new RegExp(option_sequence_regex_string);

		// Start of non-capturing group with options, e.g. (?i-U:
		var group_options_regex_string = '\\(\\?' + options_regex_string + ':';
		var group_options_regex = new RegExp(group_options_regex_string);

		// Helper functions:
		function delimiter(ch) {
			return (ch in delimiters) ? delimiters[ch] : ch;
		}
		function current(state) {
			if (!state.context.length) return false;
			return state.context[state.context.length - 1];
		}
		function consume(stream) {
			// As a nested mode, we should not consume too much so as to let the nesting mode in charge.
			// That said, eating \w is usually safe:
			if (!stream.match(/\w+/)) stream.next();
		}
		function all_tokens(state, token) {
			var result = state.context.join(' ');
			if (token) {
				// Avoid leading spaces as they confuse matchbrackets (see issue #4):
				if (result) result += ' ';
				result += token;
			}
			return result;
		}
		function push(state, new_context, new_context_state, token) {
			var ret = all_tokens(state, token);
			state.context.push(new_context);
			state.context_state.push(new_context_state || {});
			return ret;
		}
		function pop(state, token) {
			var current_context = state.context.pop();
			state.context_state.pop();
			if (token) current_context += ' ' + token;
			return all_tokens(state, current_context);
		}
		function current_context_state(state) {
			return state.context_state[state.context_state.length - 1];
		}
		function expect_name(state) {
			state.name_value = '';
			return push(state, 'name');
		}
		function expect_end(state, end_string) {
			var context_state = current_context_state(state);
			var end_string_array = [];
			for (var i = 0; i < end_string.length; ++ i) end_string_array.push(end_string[i]);
			context_state.expected = end_string_array;
			return context_state;
		}
		function read_expected_end(stream, state) {
			var expected, expected_ch, ch;
			expected = current_context_state(state).expected;
			if (expected && expected.length) {
				expected_ch = expected.shift();
				ch = stream.next();
				if (ch === expected_ch) {
					if (!expected.length) {
						return pop(state);
					}
					return all_tokens(state);
				}
				// console.log('erroneous end:', ch, 'expected:', expected_ch, 'context', current(state));
				return all_tokens(state, 'err erroneous-end-of-token');
			}
			else {
				return false;
			}
		}

		function handle_backslash(stream, state) {
			stream.eat('\\');
			if (!stream.peek()) return 'err backslash-at-end-of-pattern';

			// The backslash character has several uses. Firstly, if it is followed by a character that is not a number
			// or a letter, it takes away any special meaning that character may have.
			if (stream.match(/[^0-9a-zA-Z]/)) return 'escaped-character';

			// \Q is used to start an escaped sequence:
			if (stream.match('Q') && current(state) != 'escaped-sequence') {
				push(state, 'escaped-sequence');
				return 'escaped-sequence-start';
			}

			// \cx       "control-x", where x is any ASCII character
			if (stream.match(/c[ -~]/)) return 'non-printing-character';

			// \0dd      character with octal code 0dd
			if (stream.match(/0[0-7]{0,2}/)) return 'non-printing-character';
			// \ddd      character with octal code ddd, or back reference
			if (stream.match(/[1-7][0-7]{1,2}/)) return 'non-printing-character';
			// \o{ddd..} character with octal code ddd..
			if (stream.match(/o\{[0-7]+\}/)) return 'non-printing-character';
			// \x{hhh..} character with hex code hhh.. (non-JavaScript mode)
			if (stream.match(/x\{[0-9a-fA-F]+}/)) return 'non-printing-character';
			// \xhh      character with hex code hh
			if (stream.match(/x[0-9a-fA-F]{0,2}/)) return 'non-printing-character';
			// \uhhhh    character with hex code hhhh (JavaScript mode only)
			if (stream.match(/u[0-9a-fA-F]{4}/)) return 'non-printing-character';

			// \p{...} and \P{...}:
			var rem = stream.match(backslash_p_regex);
			if (rem) {
				if (rem[1] in backslask_p_properties) return 'generic-character-type';
				else return 'err unknown-property-name-after-p';
			}

			var in_character_class = (current(state) === 'character-class');
			// Nothing in this condition can be found in a character class:
			if (!in_character_class) {
				// The sequence \g followed by an unsigned or a negative number, optionally enclosed in braces, is an
				// absolute or relative back reference. A named back reference can be coded as \g{name}.
				if (stream.match(/g-?[0-9]+/)) return 'backreference';
				if (stream.match(/g\{-?[0-9]+\}/)) return 'backreference';
				if (stream.match(/g\{/, false)) return push(state, 'backreference');
				// \k<name>  reference by name (Perl)
				// \k'name'  reference by name (Perl)
				// \k{name}  reference by name (.NET)
				if (stream.match(/k[<'{]/, false)) return push(state, 'backreference');
				if (stream.match(/[0-9]+/)) return 'backreference';

				// For compatibility with Oniguruma, the non-Perl syntax \g followed by a name or a number enclosed either
				// in angle brackets or single quotes, is an alternative syntax for referencing a subpattern as a
				// "subroutine".
				if (stream.match(/g<[-+]?[0-9]+>/)) return 'subroutine';
				if (stream.match(/g'[-+]?[0-9]+'/)) return 'subroutine';
				if (stream.match(/g[<']/, false)) return push(state, 'subroutine');
			}
			// At this stage, we have looked for:
			//   - a backslash followed by nothing
			//   - a backslash followed by a single non-alphanumeric character
			//   - a backslash followed by 1 or more characters to achieve a special, context-dependent meaning
			// Look for a backslash followed by a single alphanumeric character:
			var backslash_p = in_character_class ? backslash_in_character_class : backslash_in_normal_context;
			return backslash_p[stream.next()];
		}

		function handle_name(stream, state) {
			var ret, rem, consume_limit;
			var ch = stream.next();
			// Names must start with a non-digit.
			if (!state.name_value.length && (!ch.match(/\w/) || ch.match(/\d/))) {
				ret = 'err erroneous-start-of-name';
				consume_limit = 0;
			}
			// Names consist of up to 32 alphanumeric characters and underscores.
			else if (state.name_value.length > 31) {
				ret = 'err name-too-long';
				consume_limit = -1;
			} else consume_limit = 32 - state.name_value.length - 1;
			state.name_value += ch;
			if (consume_limit < 0) {
				if (rem = stream.match(/^\w+/)) state.name_value += rem[0];
			} else while (consume_limit --) {
				if (rem = stream.match(/^\w/)) state.name_value += rem[0];
				else break;
			}
			var next_char = stream.peek();
			if (!next_char || !next_char.match(/\w/)) return pop(state, ret);
			return all_tokens(state, ret);
		}

		function handle_callout(stream, state) {
			// (?C)       callout
			// (?Cn)      callout with data n
			var rem = stream.match(callout_regex);
			if (rem) {
				return Number(rem[1]) < 256 ? 'callout' : 'err erroneous-callout-number';
			}
			return false;
		}

		function handle_condition_subroutines(stream, state) {
			if (stream.peek() === ')') {
				pop(state);
				return tokenBase(stream, state);
			}
			stream.eat('R');
			if (stream.eat('&')) return expect_name(state);
			stream.match(/\d+/);
			return pop(state);
		}

		function handle_conditions(stream, state) {
			var condition_state = current_context_state(state);
			var expected_end = read_expected_end(stream, state);
			if (expected_end) return expected_end;
			if (condition_state.ok) {
				pop(state);
				return tokenBase(stream, state);
			}
			// (?(DEFINE)...  define subpattern for reference
			if (stream.match(/DEFINE(?=\))/)) {
				return pop(state, 'define');
			}
			// (?(R)...   overall recursion condition
			// (?(Rn)...  specific group recursion condition
			// (?(R&name)...) specific recursion condition
			if (stream.match(/R(\d+|&\w+|)\)/, false)) {
				condition_state.ok = true;
				push(state, 'condition-subroutine');
				return tokenBase(stream, state);
			}
			// (?(n)...   absolute reference condition
			// (?(+n)...  relative reference condition
			// (?(-n)...  relative reference condition
			if (stream.match(/(-|\+|)\d+/)) {
				condition_state.ok = true;
				return all_tokens(state, 'backreference');
			}
			var rem = stream.match(/([<'])/);
			if (rem) {
				condition_state.ok = false;
				expect_end(state, delimiter(rem[1]));
				return expect_name(state);
			}
			if (stream.match(/\w+/, false)) {
				condition_state.ok = true; // the "name" state will handle everything for us
				return expect_name(state);
			}
			// If the condition is not in any of the above formats, it must be an assertion.  This may be a positive or
			// negative lookahead or lookbehind assertion.
			if (stream.match(/\?<?[=!]/)) {
				condition_state.ok = true; // the "group" state will handle everything for us
				// Ensure "group" leaves the closing parenthesis untouched so "start-group" can consume it:
				var group_options = {'leave_closing_parenthesis': true};
				return push(state, 'group' + (++ state.group_level), group_options, 'start-group');
			}
			stream.next();
			return all_tokens(state, 'err erroneous-condition');
		}

		function handle_start_group(stream, state) {
			var start_group_state = current_context_state(state);
			var expected_end = read_expected_end(stream, state);
			if (expected_end) return expected_end;
			var rem;
			if (start_group_state.option_shorthand === 1) {
				// A shorthand option was spotted, handle it:
				start_group_state.option_shorthand = 2;
				stream.match(/[^:]+/);
				return all_tokens(state, 'option-sequence');
			}
			if (start_group_state.option_shorthand === 2) {
				// A shorthand option was handled, finish the job:
				stream.eat(':');
				return pop(state);
			}
			if (start_group_state.condition_callout === 1) {
				// A pre-condition callout was spotted, handle it:
				start_group_state.condition_callout = 2;
				return all_tokens(state, handle_callout(stream, state));
			}
			if (start_group_state.condition_callout === 2) {
				// A pre-condition callout was handled, resume
				stream.eat('(');
				expect_end(state, ')');
				return push(state, 'condition');
			}
			// (?<name>...)   named capturing group (Perl)
			// (?'name'...)   named capturing group (Perl)
			// (?P<name>...)  named capturing group (Python)
			rem = stream.match(/\(\?P?([<'])/);
			if (rem) {
				expect_end(state, delimiter(rem[1]));
				return expect_name(state);
			}
			// Same as (?: but with options, e.g. (?x-i:
			if (stream.match(group_options_regex, false)) {
				// As a convenient shorthand, if any option settings are required at the start of a non-capturing
				// subpattern, the option letters may appear between the "?" and the ":".
				stream.match('(?');
				start_group_state.option_shorthand = 1;
				return all_tokens(state);
			}
			// "(?(" typically marks the start of a condition: (?(condition)yes-pattern|no-pattern)
			if (stream.match('(?') && stream.peek() === '(') {
				// An explicit callout may be set just before an assertion condition: (?(?C7)(?<!abc)def|ghi)
				start_group_state.condition_callout = (stream.match(condition_callout_regex, false)) ? 1 : 2;
				return all_tokens(state);
			}
			stream.next();
			return all_tokens(state, 'err erroneous-start-of-start-group');
		}

		function handle_backreference(stream, state) {
			var expected_end = read_expected_end(stream, state);
			if (expected_end) return expected_end;
			var rem = stream.match(/k([<'{])/) || stream.match(/g(\{)/) || stream.match(/(\()\?P=/);
			if (rem) {
				expect_end(state, delimiter(rem[1]));
				return expect_name(state);
			}
			stream.next();
			return all_tokens(state, 'err erroneous-backreference');
		}

		function handle_subroutine(stream, state) {
			var expected_end = read_expected_end(stream, state);
			if (expected_end) return expected_end;
			var rem = stream.match(/g([<'])/) || stream.match(/(\()\?(P>|&)/);
			if (rem) {
				expect_end(state, delimiter(rem[1]));
				return expect_name(state);
			}
			stream.next();
			return all_tokens(state, 'err erroneous-subroutine');
		}

		function handle_verb(stream, state) {
			var expected_end = read_expected_end(stream, state);
			if (expected_end) return expected_end;
			expect_end(state, ')');
			return expect_name(state);
		}

		function update_options(state, options) {
			// We are only interested in x (extended mode).
			var enable = true, new_state = null, i = 0, c = null;
			for (; i < options.length; ++i) {
				c = options[i];
				if (c === '-') enable = false;
				else if (c === 'x') new_state = enable;
			}
			if (new_state !== null) state.extended = new_state;
		}

		function tokenBase(stream, state) {
			var rem, ret; // stand for Regular Expression Match and RETurn, respectively.

			// Get current state, current char, next char:
			var ch = stream.peek();
			if (!ch) return;
			var current_state = current(state);
			var group_state;

			if (current_state === 'name') return handle_name(stream, state);
			if (current_state === 'condition') return handle_conditions(stream, state);
			if (current_state === 'condition-subroutine') return handle_condition_subroutines(stream, state);
			if (current_state === 'start-group') return handle_start_group(stream, state);
			if (current_state === 'backreference') return handle_backreference(stream, state);
			if (current_state === 'subroutine') return handle_subroutine(stream, state);
			if (current_state === 'verb') return handle_verb(stream, state);

			if (current_state === 'escaped-sequence') {
				if (stream.match('\\E')) return pop(state, 'escaped-sequence-end');
				consume(stream);
				return all_tokens(state);
			}

			// Escaped characters:
			if (stream.match(/\\./, false)) return all_tokens(state, handle_backslash(stream, state));

			if (stream.match('[', false)) {
				if (current_state !== 'character-class') {
					if (stream.match(posix_named_sets_regex)) {
						return all_tokens(state, 'err posix-outside-class-unsupported');
					}
					// In the POSIX.2 compliant library that was included in 4.4BSD Unix, the ugly syntax [[:<:]] and
					// [[:>:]] is used for matching "start of word" and "end of word".
					if (stream.match('[[:<:]]') || stream.match('[[:>:]]')) return all_tokens(state, 'anchor');
					// At this stage, we do have a new character class:
					push(state, 'character-class');
					stream.eat('[');
					stream.eat('^');
					// If a closing square bracket is required as a member of the class, it should be the first data
					// character in the class (after an initial circumflex, if present) or escaped with a backslash.
					// Note: ']' should be on the same line as '[', even in extended mode.
					stream.eat(']');
					return all_tokens(state);
				}
			}

			if (current_state === 'character-class') {
				rem = stream.match(posix_named_sets_regex);
				if (rem) {
					if (rem[1] in posix_named_sets) return all_tokens(state, 'generic-character-type');
					else return all_tokens(state, 'err unknown-posix-class-name');
				}
				if (stream.eat(']')) return pop(state);
				consume(stream);
				return all_tokens(state);
			}

			// Regular comments in extended mode:
			if (state.extended && stream.eat('#')) {
				stream.skipToEnd();
				return 'comment';
			}

			if (stream.eat('{')) {
				// exactly n:
				if (stream.match(/\d+\}/)) return all_tokens(state, 'quantifier');
				// "at least n, no more than m" and "n or more", greedy, possessive or lazy:
				if (stream.match(/\d+,\d*\}[+?]?/)) return all_tokens(state, 'quantifier');
			}

			if (stream.eat('|')) {
				return all_tokens(state, 'alternation');
			}

			if (stream.peek() === '(') {
				if (stream.match(/\(\*(?:UTF(?:8|16|32|)|UCP|NO_AUTO_POSSESS|NO_START_OPT)\)/)) return all_tokens(state, 'option-sequence');
				if (stream.match(/\(\*LIMIT_(?:RECURSION|MATCH)=[0-9]+\)/)) return all_tokens(state, 'option-sequence');
				// Newline convention + what \R matches:
				if (stream.match(/\(\*(?:CR|LF|CRLF|ANYCRLF|ANY|BSR_(?:ANYCRLF|UNICODE))\)/)) return all_tokens(state, 'option-sequence');
				// Backtracking control:
				if (stream.match(/\(\*(?:ACCEPT|FAIL|F|COMMIT|PRUNE|SKIP|THEN)\)/)) return all_tokens(state, 'verb');
				if (stream.match(/\(\*(?:MARK|PRUNE|SKIP|THEN|):/)) return push(state, 'verb', {}, 'verb');
				rem = stream.match(option_sequence_regex);
				if (rem) {
					update_options(state, rem[0]);
					return all_tokens(state, 'option-sequence');
				}
				// (?#....)  comment (not nestable)
				if (stream.match(/\(\?#[^)]*\)/)) return all_tokens(state, 'comment');
				// (?P=name)  reference by name (Python)
				if (stream.match(/\(\?P=/, false)) return push(state, 'backreference');
				// (?&name)   call subpattern by name (Perl)
				// (?P>name)  call subpattern by name (Python)
				if (stream.match(/\(\?(P>|&)/, false)) return push(state, 'subroutine');
				// (?n)       call subpattern by absolute number
				// (?+n)      call subpattern by relative number
				// (?-n)      call subpattern by relative number
				if (stream.match(/\(\?(\-|\+|)\d+\)/)) return all_tokens(state, 'subroutine');
				// (?R)       recurse whole pattern
				if (stream.match('(?R)')) return all_tokens(state, 'subroutine');
				// Callouts:
				var callout = handle_callout(stream, state);
				if (callout) return all_tokens(state, callout);

				// At this stage, we have a new group:
				++ state.group_level;
				group_state = 'group' + state.group_level;
				push(state, group_state);

				// (?=...)   positive look ahead
				// (?!...)   negative look ahead
				// (?<=...)  positive look behind
				// (?<!...)  negative look behind
				if (stream.match(assertion_regex)) return all_tokens(state, 'start-group');
				// (?:...)   non-capturing group
				// (?|...)   non-capturing group; reset group numbers for capturing groups in each alternative
				// (?>...)   atomic, non-capturing group
				if (stream.match(/\(\?[:|>]/)) return all_tokens(state, 'start-group');

				if (stream.match('(?', false)) {
					push(state, 'start-group');
					return tokenBase(stream, state);
				}
				stream.eat('(');
				return all_tokens(state, 'start-group');
			}

			if (stream.peek() === ')') {
				if (current_state && current_state.match(/^group/)) {
					ret = 'start-group'; // formerly 'end-group' but that used to confuse matchbrackets (see issue #4)
					if (current_context_state(state).leave_closing_parenthesis) ret = '';
					else stream.next();
					-- state.group_level;
					return pop(state, ret);
				}
				stream.next();
				return all_tokens(state, 'err unmatched-closing-parenthesis');
			}

			// Anchors
			if (stream.eat('^') || stream.eat('$')) return all_tokens(state, 'anchor');
			if (stream.eat('.')) return all_tokens(state, 'generic-character-type');
			// Quantifiers: 0 or 1, 0 or more, 1 or more, greedy:
			if (stream.eat('?') || stream.eat('*') || stream.eat('+')) {
				// Handle possessive and lazy variants:
				stream.eat(/[+?]/);
				return all_tokens(state, 'quantifier');
			}
			consume(stream);
			return all_tokens(state);
		}

		function startState() {
			return {
				context: [],
				context_state: [],
				group_level: 0,
				name_value: '',
				extended: options.extended,
			};
		}

		function copyState(o) { // o = original
			var i, oo, oc, key, c = startState(); // c = copy, oo = original object, oc = object copy
			for (i = 0; i < o.context_state.length; ++i) {
				oo = o.context_state[i];
				oc = {};
				for (key in oo) oc[key] = (key === 'expected') ? oo[key].slice() : oo[key];
				c.context_state.push(oc);
			}
			c.context = o.context.slice();
			c.group_level = o.group_level;
			c.name_value = o.name_value;
			c.extended = o.extended;
			return c;
		}

		return {
			startState: startState,
			copyState: copyState,
			token: tokenBase,
		};
	});

	CodeMirror.defineMIME('text/x-regex', 'pcre');
	CodeMirror.defineMIME('text/x-pcre-regex', 'pcre');

});
