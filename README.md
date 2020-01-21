# SVG non-stop
## What is this

Java utility fixes `Gradient has no stop info` error when importing SVG files as vector assets in Android studio. This error results in corrupt drawable with empty gradient fills.

## How to use

1. Download binary from [releases/latest](https://github.com/14v/svg-non-stop/releases/latest)

2. Pass target SVG file name as parameter:

    ```
    ./bin/svg-non-stop my_vector_drawable.svg
    ```

3. Result will be saved in `my_vector_drawable_nonstop.svg`. Use this file to import in IDE.

Optional parameters: 
* `-f` to force overwrite target `_nonstop.svg` file, 
* `-v` to have verbose output.

## Technical notes
Utility copies stops definitions into target gradients in SVG file. Then IDE can process file correctly. Utility is _not_ for use in android code.

For build use gradle `assemble` task and you will get `build/distributions/svg-non-stop-x.y.z.zip` archive.
