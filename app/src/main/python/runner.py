import io
import contextlib
import traceback

def run(code, provider):
    output = io.StringIO()
    errors = io.StringIO()

    def malz_input(prompt=""):
        return provider.readLine(str(prompt))

    scope = {
        "__name__": "__main__",
        "__file__": "<malzpy>",
        "input": malz_input,
    }

    try:
        with contextlib.redirect_stdout(output), contextlib.redirect_stderr(errors):
            exec(compile(code, "<malzpy>", "exec"), scope, scope)
    except BaseException:
        traceback.print_exc(file=errors)

    return output.getvalue() + errors.getvalue()
