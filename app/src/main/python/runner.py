import io
import contextlib
import traceback

def run(code):
    output = io.StringIO()
    errors = io.StringIO()

    def blocked_input(prompt=""):
        raise RuntimeError("input() belum didukung di terminal MalzPy")

    scope = {
        "__name__": "__main__",
        "__file__": "<malzpy>",
        "input": blocked_input,
    }

    try:
        with contextlib.redirect_stdout(output), contextlib.redirect_stderr(errors):
            exec(compile(code, "<malzpy>", "exec"), scope, scope)
    except BaseException:
        traceback.print_exc(file=errors)

    return output.getvalue() + errors.getvalue()
