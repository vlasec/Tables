# Tables

This library allows you to define table-based data with compile-time type safety.

A table consists of the following components:

## Tables

`Table` defines the ability to read a `Row` from `Source` and ability to create a `View` as a projection that is used
to create output collections and output its headers as well.

Any further definition of the table's data is up to the user of this library, as tables can be materialized or they can
use various forms of streaming if the data is big. None of this is covered in the library.

## Views

`View` is the component that controls the output of a row. It filters `Table`'s columns and only outputs headers and
data for columns that are retained by the filter.

Filtration of based on the contents of rows is not covered by the library, but `Row` provides useful tools for that,
should there be need for value-based filtering.

## Rows

`Row` is merely a container for data, and it is a product of a `Table`.

It can be transformed into output format by a `View`. It can also provide a column value by its `optGet` method.

## Columns

The idea revolves around defining individual columns of a table-based structure, and building tables from these
columns used as building blocks. No special DSL is used to produce these columns, as their implementation can differ,
e.g. one can use libraries like Enumeratum to make the column definitions more enum-like.

A column, in its core, is just a typed and named extractor of data.

### TypedColumn

A column that extracts `Target` from its `Source`. It can either be a direct column with no intermediary types,
or a result of macro expansion where intermediary type is encapsulated in its `optRead` function.

A column is optional by default, but if it's convenient, `Required` mixin can be extended, defining the `optRead`
using `read` method, thus making definition simpler where the data is not optional.

### TypedMacroColumn

This column is still a `TypedColumnDefinition`, but for clarity, it calls its target type `Flow`, as it is not the
target object meant for output, but an intermediary object that serves as a `Source` for other columns.

A macro column is a definition that can be expanded in multiple columns.

The columns to be embedded need to be of the intermediary `Flow` type, and both simple columns like`TypedOptionalColumn`
and macros are supported within `columnDefinitions` method. The embedded macros get expanded as well. Make sure not to
include any kind of infinite recursion or cycle, it would result in a `StackOverflowError`.

The `optRead` functions get composed, the resulting `EmbeddedColumn`

The headers are composed using `composeHeaders` function, which by default prepends the macro header to the header
of embedded column. This behavior can be overridden if you prefer it to behave differently.

## Identifiers

A `TypedColumn` serves as its own identifier in a `Row` or a `View`.

Any identifier can also be prepended with a `MacroColumn` to create a `CompositeTypeIdentifier`.

### CompositeTypedIdentifier

A composite identifier uses macro columns and like them, it calls the intermediary object `Flow`. To create a composite
identifier, you can just prepend an identifier with a macro column using list-like `::` syntax.

```scala
MyMacroColumn :: LeafColumn
```

Identifiers also provide `contains` method, mostly as a useful tool for filtering.

# Example

For better understanding, [Example spec](src/test/scala/cz/vlasec/tables/example/ExampleSpec.scala) is recommended,
it shows a bit more extreme macro expansion with a rather large resulting table and some views that narrow it down.

