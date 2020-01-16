Java utility fixes "Gradient has no stop info" when importing SVG files as vector assets in Android studio.

Pass target SVG file name as parameter.

Optional parameters: -f to force overwrite target _nonstop.svg file, -v to have verbose output.

Example:
```
java -jar svg-non-stop-1.0.jar my_vector_drawable.svg
```