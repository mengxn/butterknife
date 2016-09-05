package butterknife;

import butterknife.compiler.ButterKnifeProcessor;
import com.google.testing.compile.JavaFileObjects;
import javax.tools.JavaFileObject;
import org.junit.Test;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static java.util.Arrays.asList;

public class UnbinderTest {
  @Test public void multipleBindings() {
    JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
        + "package test;\n"
        + "import android.app.Activity;\n"
        + "import android.view.View;\n"
        + "import butterknife.BindView;\n"
        + "import butterknife.OnClick;\n"
        + "import butterknife.OnLongClick;\n"
        + "public class Test extends Activity {\n"
        + "  @BindView(1) View view;\n"
        + "  @BindView(2) View view2;\n"
        + "  @OnClick(1) void doStuff() {}\n"
        + "  @OnLongClick(1) boolean doMoreStuff() { return false; }\n"
        + "}"
    );

    JavaFileObject bindingSource = JavaFileObjects.forSourceString("test/Test_ViewBinding", ""
        + "package test;\n"
        + "import android.support.annotation.CallSuper;\n"
        + "import android.support.annotation.UiThread;\n"
        + "import android.view.View;\n"
        + "import butterknife.Unbinder;\n"
        + "import butterknife.internal.DebouncingOnClickListener;\n"
        + "import butterknife.internal.Utils;\n"
        + "import java.lang.IllegalStateException;\n"
        + "import java.lang.Override;\n"
        + "public class Test_ViewBinding<T extends Test> implements Unbinder {\n"
        + "  protected T target;\n"
        + "  private View view1;\n"
        + "  @UiThread\n"
        + "  public Test_ViewBinding(final T target, View source) {\n"
        + "    this.target = target;\n"
        + "    View view;\n"
        + "    view = Utils.findRequiredView(source, 1, \"field 'view', method 'doStuff', and method 'doMoreStuff'\");\n"
        + "    target.view = view;\n"
        + "    view1 = view;\n"
        + "    view.setOnClickListener(new DebouncingOnClickListener() {\n"
        + "      @Override\n"
        + "      public void doClick(View p0) {\n"
        + "        target.doStuff();\n"
        + "      }\n"
        + "    });\n"
        + "    view.setOnLongClickListener(new View.OnLongClickListener() {\n"
        + "      @Override\n"
        + "      public boolean onLongClick(View p0) {\n"
        + "        return target.doMoreStuff();\n"
        + "      }\n"
        + "    });\n"
        + "    target.view2 = Utils.findRequiredView(source, 2, \"field 'view2'\");\n"
        + "  }\n"
        + "  @Override\n"
        + "  @CallSuper\n"
        + "  public void unbind() {\n"
        + "    T target = this.target;\n"
        + "    if (target == null) throw new IllegalStateException(\"Bindings already cleared.\");\n"
        + "    target.view = null;\n"
        + "    target.view2 = null;\n"
        + "    view1.setOnClickListener(null);\n"
        + "    view1.setOnLongClickListener(null);\n"
        + "    view1 = null;\n"
        + "    this.target = null;\n"
        + "  }\n"
        + "}"
    );

    assertAbout(javaSource()).that(source)
        .withCompilerOptions("-Xlint:-processing")
        .processedWith(new ButterKnifeProcessor())
        .compilesWithoutWarnings()
        .and()
        .generatesSources(bindingSource);
  }

  @Test public void unbinderRespectsNullable() {
    JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
        + "package test;\n"
        + "import android.app.Activity;\n"
        + "import butterknife.OnClick;\n"
        + "import butterknife.Optional;\n"
        + "public class Test extends Activity {\n"
        + "  @Optional @OnClick(1) void doStuff() {}\n"
        + "}"
    );

    JavaFileObject bindingSource = JavaFileObjects.forSourceString("test/Test_ViewBinding", ""
        + "package test;\n"
        + "import android.support.annotation.CallSuper;\n"
        + "import android.support.annotation.UiThread;\n"
        + "import android.view.View;\n"
        + "import butterknife.Unbinder;\n"
        + "import butterknife.internal.DebouncingOnClickListener;\n"
        + "import java.lang.IllegalStateException;\n"
        + "import java.lang.Override;\n"
        + "public class Test_ViewBinding<T extends Test> implements Unbinder {\n"
        + "  protected T target;\n"
        + "  private View view1;\n"
        + "  @UiThread\n"
        + "  public Test_ViewBinding(final T target, View source) {\n"
        + "    this.target = target;\n"
        + "    View view;\n"
        + "    view = source.findViewById(1);\n"
        + "    if (view != null) {\n"
        + "      view1 = view;\n"
        + "      view.setOnClickListener(new DebouncingOnClickListener() {\n"
        + "        @Override\n"
        + "        public void doClick(View p0) {\n"
        + "          target.doStuff();\n"
        + "        }\n"
        + "      });\n"
        + "    }\n"
        + "  }\n"
        + "  @Override\n"
        + "  @CallSuper\n"
        + "  public void unbind() {\n"
        + "    if (this.target == null) throw new IllegalStateException(\"Bindings already cleared.\");\n"
        + "    if (view1 != null) {\n"
        + "      view1.setOnClickListener(null);\n"
        + "      view1 = null;\n"
        + "    }\n"
        + "    this.target = null;\n"
        + "  }\n"
        + "}"
    );

    assertAbout(javaSource()).that(source)
        .withCompilerOptions("-Xlint:-processing")
        .processedWith(new ButterKnifeProcessor())
        .compilesWithoutWarnings()
        .and()
        .generatesSources(bindingSource);
  }

  @Test public void childBindsSecondUnbinder() {
    JavaFileObject source1 = JavaFileObjects.forSourceString("test.Test", ""
        + "package test;\n"
        + "import android.app.Activity;\n"
        + "import butterknife.OnClick;\n"
        + "public class Test extends Activity {\n"
        + "  @OnClick(1) void doStuff1() {}\n"
        + "}"
    );

    JavaFileObject source2 = JavaFileObjects.forSourceString("test.TestOne", ""
        + "package test;\n"
        + "import butterknife.OnClick;\n"
        + "public class TestOne extends Test {\n"
        + "  @OnClick(1) void doStuff2() {}\n"
        + "}"
    );

    JavaFileObject source3 = JavaFileObjects.forSourceString("test.TestTwo", ""
        + "package test;\n"
        + "class TestTwo extends Test {}"
    );

    JavaFileObject binding1Source = JavaFileObjects.forSourceString("test/Test_ViewBinding", ""
        + "package test;\n"
        + "import android.support.annotation.CallSuper;\n"
        + "import android.support.annotation.UiThread;\n"
        + "import android.view.View;\n"
        + "import butterknife.Unbinder;\n"
        + "import butterknife.internal.DebouncingOnClickListener;\n"
        + "import butterknife.internal.Utils;\n"
        + "import java.lang.IllegalStateException;\n"
        + "import java.lang.Override;\n"
        + "public class Test_ViewBinding<T extends Test> implements Unbinder {\n"
        + "  protected T target;\n"
        + "  private View view1;\n"
        + "  @UiThread\n"
        + "  public Test_ViewBinding(final T target, View source) {\n"
        + "    this.target = target;\n"
        + "    View view;\n"
        + "    view = Utils.findRequiredView(source, 1, \"method 'doStuff1'\");\n"
        + "    view1 = view;\n"
        + "    view.setOnClickListener(new DebouncingOnClickListener() {\n"
        + "      @Override\n"
        + "      public void doClick(View p0) {\n"
        + "        target.doStuff1();\n"
        + "      }\n"
        + "    });\n"
        + "  }\n"
        + "  @Override\n"
        + "  @CallSuper\n"
        + "  public void unbind() {\n"
        + "    if (this.target == null) throw new IllegalStateException(\"Bindings already cleared.\");\n"
        + "    view1.setOnClickListener(null);\n"
        + "    view1 = null;\n"
        + "    this.target = null;\n"
        + "  }\n"
        + "}"
    );

    JavaFileObject binding2Source = JavaFileObjects.forSourceString("test/TestOne_ViewBinding", ""
        + "package test;\n"
        + "import android.support.annotation.UiThread;\n"
        + "import android.view.View;\n"
        + "import butterknife.internal.DebouncingOnClickListener;\n"
        + "import butterknife.internal.Utils;\n"
        + "import java.lang.Override;\n"
        + "public class TestOne_ViewBinding<T extends TestOne> extends Test_ViewBinding<T> {\n"
        + "  private View view1;\n"
        + "  @UiThread\n"
        + "  public TestOne_ViewBinding(final T target, View source) {\n"
        + "    super(target, source);\n"
        + "    View view;\n"
        + "    view = Utils.findRequiredView(source, 1, \"method 'doStuff2'\");\n"
        + "    view1 = view;\n"
        + "    view.setOnClickListener(new DebouncingOnClickListener() {\n"
        + "      @Override\n"
        + "      public void doClick(View p0) {\n"
        + "        target.doStuff2();\n"
        + "      }\n"
        + "    });\n"
        + "  }\n"
        + "  @Override\n"
        + "  public void unbind() {\n"
        + "    super.unbind();\n"
        + "    view1.setOnClickListener(null);\n"
        + "    view1 = null;\n"
        + "  }\n"
        + "}"
    );

    assertAbout(javaSources()).that(asList(source1, source2, source3))
        .withCompilerOptions("-Xlint:-processing")
        .processedWith(new ButterKnifeProcessor())
        .compilesWithoutWarnings()
        .and()
        .generatesSources(binding1Source, binding2Source);
  }

  @Test public void childUsesOwnUnbinder() {
    JavaFileObject source1 = JavaFileObjects.forSourceString("test.Test", ""
        + "package test;\n"
        + "import android.app.Activity;\n"
        + "import butterknife.OnClick;\n"
        + "public class Test extends Activity {\n"
        + "  @OnClick(1) void doStuff1() { }\n"
        + "}"
    );

    JavaFileObject source2 = JavaFileObjects.forSourceString("test.TestOne", ""
        + "package test;\n"
        + "import butterknife.OnClick;\n"
        + "public class TestOne extends Test {\n"
        + "  @OnClick(1) void doStuff2() { }\n"
        + "}"
    );

    JavaFileObject binding1Source = JavaFileObjects.forSourceString("test/Test_ViewBinding", ""
        + "package test;\n"
        + "import android.support.annotation.CallSuper;\n"
        + "import android.support.annotation.UiThread;\n"
        + "import android.view.View;\n"
        + "import butterknife.Unbinder;\n"
        + "import butterknife.internal.DebouncingOnClickListener;\n"
        + "import butterknife.internal.Utils;\n"
        + "import java.lang.IllegalStateException;\n"
        + "import java.lang.Override;\n"
        + "public class Test_ViewBinding<T extends Test> implements Unbinder {\n"
        + "  protected T target;\n"
        + "  private View view1;\n"
        + "  @UiThread\n"
        + "  public Test_ViewBinding(final T target, View source) {\n"
        + "    this.target = target;\n"
        + "    View view;\n"
        + "    view = Utils.findRequiredView(source, 1, \"method 'doStuff1'\");\n"
        + "    view1 = view;\n"
        + "    view.setOnClickListener(new DebouncingOnClickListener() {\n"
        + "      @Override\n"
        + "      public void doClick(View p0) {\n"
        + "        target.doStuff1();\n"
        + "      }\n"
        + "    });\n"
        + "  }\n"
        + "  @Override\n"
        + "  @CallSuper\n"
        + "  public void unbind() {\n"
        + "    if (this.target == null) throw new IllegalStateException(\"Bindings already cleared.\");\n"
        + "    view1.setOnClickListener(null);\n"
        + "    view1 = null;\n"
        + "    this.target = null;\n"
        + "  }\n"
        + "}"
    );

    JavaFileObject binding2Source = JavaFileObjects.forSourceString("test/TestOne_ViewBinding", ""
        + "package test;\n"
        + "import android.support.annotation.UiThread;\n"
        + "import android.view.View;\n"
        + "import butterknife.internal.DebouncingOnClickListener;\n"
        + "import butterknife.internal.Utils;\n"
        + "import java.lang.Override;\n"
        + "public class TestOne_ViewBinding<T extends TestOne> extends Test_ViewBinding<T> {\n"
        + "  private View view1;\n"
        + "  @UiThread\n"
        + "  public TestOne_ViewBinding(final T target, View source) {\n"
        + "    super(target, source);\n"
        + "    View view;\n"
        + "    view = Utils.findRequiredView(source, 1, \"method 'doStuff2'\");\n"
        + "    view1 = view;\n"
        + "    view.setOnClickListener(new DebouncingOnClickListener() {\n"
        + "      @Override\n"
        + "      public void doClick(View p0) {\n"
        + "        target.doStuff2();\n"
        + "      }\n"
        + "    });\n"
        + "  }\n"
        + "  @Override\n"
        + "  public void unbind() {\n"
        + "    super.unbind();\n"
        + "    view1.setOnClickListener(null);\n"
        + "    view1 = null;\n"
        + "  }\n"
        + "}"
    );

    assertAbout(javaSources()).that(asList(source1, source2))
        .withCompilerOptions("-Xlint:-processing")
        .processedWith(new ButterKnifeProcessor())
        .compilesWithoutWarnings()
        .and()
        .generatesSources(binding1Source, binding2Source);
  }

  @Test public void childInDifferentPackage() {
    JavaFileObject source1 = JavaFileObjects.forSourceString("test.Test", ""
        + "package test;\n"
        + "import android.app.Activity;\n"
        + "import butterknife.OnClick;\n"
        + "public class Test extends Activity {\n"
        + "  @OnClick(1) void doStuff1() { }\n"
        + "}"
    );

    JavaFileObject source2 = JavaFileObjects.forSourceString("test.one.TestOne", ""
        + "package test.one;\n"
        + "import test.Test;\n"
        + "import butterknife.OnClick;\n"
        + "class TestOne extends Test {\n"
        + "  @OnClick(2) void doStuff2() { }\n"
        + "}"
    );

    JavaFileObject binding1Source = JavaFileObjects.forSourceString("test/Test_ViewBinding", ""
        + "package test;\n"
        + "import android.support.annotation.CallSuper;\n"
        + "import android.support.annotation.UiThread;\n"
        + "import android.view.View;\n"
        + "import butterknife.Unbinder;\n"
        + "import butterknife.internal.DebouncingOnClickListener;\n"
        + "import butterknife.internal.Utils;\n"
        + "import java.lang.IllegalStateException;\n"
        + "import java.lang.Override;\n"
        + "public class Test_ViewBinding<T extends Test> implements Unbinder {\n"
        + "  protected T target;\n"
        + "  private View view1;\n"
        + "  @UiThread\n"
        + "  public Test_ViewBinding(final T target, View source) {\n"
        + "    this.target = target;\n"
        + "    View view;\n"
        + "    view = Utils.findRequiredView(source, 1, \"method 'doStuff1'\");\n"
        + "    view1 = view;\n"
        + "    view.setOnClickListener(new DebouncingOnClickListener() {\n"
        + "      @Override\n"
        + "      public void doClick(View p0) {\n"
        + "        target.doStuff1();\n"
        + "      }\n"
        + "    });\n"
        + "  }\n"
        + "  @Override\n"
        + "  @CallSuper\n"
        + "  public void unbind() {\n"
        + "    if (this.target == null) throw new IllegalStateException(\"Bindings already cleared.\");\n"
        + "    view1.setOnClickListener(null);\n"
        + "    view1 = null;\n"
        + "    this.target = null;\n"
        + "  }\n"
        + "}"
    );

    JavaFileObject binding2Source =
        JavaFileObjects.forSourceString("test/one/TestOne_ViewBinding", ""
            + "package test.one;\n"
            + "import android.support.annotation.UiThread;\n"
            + "import android.view.View;\n"
            + "import butterknife.internal.DebouncingOnClickListener;\n"
            + "import butterknife.internal.Utils;\n"
            + "import java.lang.Override;\n"
            + "import test.Test_ViewBinding;\n"
            + "public class TestOne_ViewBinding<T extends TestOne> extends Test_ViewBinding<T> {\n"
            + "  private View view2;\n"
            + "  @UiThread\n"
            + "  public TestOne_ViewBinding(final T target, View source) {\n"
            + "    super(target, source);\n"
            + "    View view;\n"
            + "    view = Utils.findRequiredView(source, 2, \"method 'doStuff2'\");\n"
            + "    view2 = view;\n"
            + "    view.setOnClickListener(new DebouncingOnClickListener() {\n"
            + "      @Override\n"
            + "      public void doClick(View p0) {\n"
            + "        target.doStuff2();\n"
            + "      }\n"
            + "    });\n"
            + "  }\n"
            + "  @Override\n"
            + "  public void unbind() {\n"
            + "    super.unbind();\n"
            + "    view2.setOnClickListener(null);\n"
            + "    view2 = null;\n"
            + "  }\n"
            + "}"
        );

    assertAbout(javaSources()).that(asList(source1, source2))
        .withCompilerOptions("-Xlint:-processing")
        .processedWith(new ButterKnifeProcessor())
        .compilesWithoutWarnings()
        .and()
        .generatesSources(binding1Source, binding2Source);
  }

  @Test public void unbindingThroughAbstractChild() {
    JavaFileObject source1 = JavaFileObjects.forSourceString("test.Test", ""
        + "package test;\n"
        + "import android.app.Activity;\n"
        + "import butterknife.OnClick;\n"
        + "public class Test extends Activity {\n"
        + "  @OnClick(1) void doStuff1() { }\n"
        + "}"
    );

    JavaFileObject source2 = JavaFileObjects.forSourceString("test.TestOne", ""
        + "package test;\n"
        + "public class TestOne extends Test {\n"
        + "}"
    );

    JavaFileObject source3 = JavaFileObjects.forSourceString("test.TestTwo", ""
        + "package test;\n"
        + "import butterknife.OnClick;\n"
        + "class TestTwo extends TestOne {\n"
        + "  @OnClick(1) void doStuff2() { }\n"
        + "}"
    );

    JavaFileObject binding1Source = JavaFileObjects.forSourceString("test/Test_ViewBinding", ""
        + "package test;\n"
        + "import android.support.annotation.CallSuper;\n"
        + "import android.support.annotation.UiThread;\n"
        + "import android.view.View;\n"
        + "import butterknife.Unbinder;\n"
        + "import butterknife.internal.DebouncingOnClickListener;\n"
        + "import butterknife.internal.Utils;\n"
        + "import java.lang.IllegalStateException;\n"
        + "import java.lang.Override;\n"
        + "public class Test_ViewBinding<T extends Test> implements Unbinder {\n"
        + "  protected T target;\n"
        + "  private View view1;\n"
        + "  @UiThread\n"
        + "  public Test_ViewBinding(final T target, View source) {\n"
        + "    this.target = target;\n"
        + "    View view;\n"
        + "    view = Utils.findRequiredView(source, 1, \"method 'doStuff1'\");\n"
        + "    view1 = view;\n"
        + "    view.setOnClickListener(new DebouncingOnClickListener() {\n"
        + "      @Override\n"
        + "      public void doClick(View p0) {\n"
        + "        target.doStuff1();\n"
        + "      }\n"
        + "    });\n"
        + "  }\n"
        + "  @Override\n"
        + "  @CallSuper\n"
        + "  public void unbind() {\n"
        + "    if (this.target == null) throw new IllegalStateException(\"Bindings already cleared.\");\n"
        + "    view1.setOnClickListener(null);\n"
        + "    view1 = null;\n"
        + "    this.target = null;\n"
        + "  }\n"
        + "}"
    );

    JavaFileObject binding2Source = JavaFileObjects.forSourceString("test/TestTwo_ViewBinding", ""
        + "package test;\n"
        + "import android.support.annotation.UiThread;\n"
        + "import android.view.View;\n"
        + "import butterknife.internal.DebouncingOnClickListener;\n"
        + "import butterknife.internal.Utils;\n"
        + "import java.lang.Override;\n"
        + "public class TestTwo_ViewBinding<T extends TestTwo> extends Test_ViewBinding<T> {\n"
        + "  private View view1;\n"
        + "  @UiThread\n"
        + "  public TestTwo_ViewBinding(final T target, View source) {\n"
        + "    super(target, source);\n"
        + "    View view;\n"
        + "    view = Utils.findRequiredView(source, 1, \"method 'doStuff2'\");\n"
        + "    view1 = view;\n"
        + "    view.setOnClickListener(new DebouncingOnClickListener() {\n"
        + "      @Override\n"
        + "      public void doClick(View p0) {\n"
        + "        target.doStuff2();\n"
        + "      }\n"
        + "    });\n"
        + "  }\n"
        + "  @Override\n"
        + "  public void unbind() {\n"
        + "    super.unbind();\n"
        + "    view1.setOnClickListener(null);\n"
        + "    view1 = null;\n"
        + "  }\n"
        + "}"
    );

    assertAbout(javaSources()).that(asList(source1, source2, source3))
        .withCompilerOptions("-Xlint:-processing")
        .processedWith(new ButterKnifeProcessor())
        .compilesWithoutWarnings()
        .and()
        .generatesSources(binding1Source, binding2Source);
  }

  @Test public void fullIntegration() {
    JavaFileObject sourceA = JavaFileObjects.forSourceString("test.A", ""
        + "package test;\n"
        + "import android.support.annotation.ColorInt;\n"
        + "import android.view.View;\n"
        + "import butterknife.BindColor;\n"
        + "import butterknife.ButterKnife;\n"
        + "public class A {\n"
        + "  @BindColor(android.R.color.black) @ColorInt int blackColor;\n"
        + "  public A(View view) {\n"
        + "    ButterKnife.bind(this, view);\n"
        + "  }\n"
        + "}\n");

    JavaFileObject sourceB = JavaFileObjects.forSourceString("test.B", ""
        + "package test;\n"
        + "import android.support.annotation.ColorInt;\n"
        + "import android.view.View;\n"
        + "import butterknife.BindColor;\n"
        + "import butterknife.ButterKnife;\n"
        + "public class B extends A {\n"
        + "  @BindColor(android.R.color.white) @ColorInt int whiteColor;\n"
        + "  public B(View view) {\n"
        + "    super(view);\n"
        + "    ButterKnife.bind(this, view);\n"
        + "  }\n"
        + "}\n");

    JavaFileObject sourceC = JavaFileObjects.forSourceString("test.C", ""
        + "package test;\n"
        + "import android.support.annotation.ColorInt;\n"
        + "import android.view.View;\n"
        + "import butterknife.BindView;\n"
        + "import butterknife.BindColor;\n"
        + "import butterknife.ButterKnife;\n"
        + "public class C extends B {\n"
        + "  @BindColor(android.R.color.transparent) @ColorInt int transparentColor;\n"
        + "  @BindView(android.R.id.button1) View button1;\n"
        + "  public C(View view) {\n"
        + "    super(view);\n"
        + "    ButterKnife.bind(this, view);\n"
        + "  }\n"
        + "}\n");

    JavaFileObject sourceD = JavaFileObjects.forSourceString("test.D", ""
        + "package test;\n"
        + "import android.support.annotation.ColorInt;\n"
        + "import android.view.View;\n"
        + "import butterknife.BindColor;\n"
        + "import butterknife.ButterKnife;\n"
        + "public class D extends C {\n"
        + "  @BindColor(android.R.color.darker_gray) @ColorInt int grayColor;\n"
        + "  public D(View view) {\n"
        + "    super(view);\n"
        + "    ButterKnife.bind(this, view);\n"
        + "  }\n"
        + "}\n");

    JavaFileObject sourceE = JavaFileObjects.forSourceString("test.E", ""
        + "package test;\n"
        + "import android.support.annotation.ColorInt;\n"
        + "import android.view.View;\n"
        + "import butterknife.BindColor;\n"
        + "import butterknife.ButterKnife;\n"
        + "public class E extends C {\n"
        + "  @BindColor(android.R.color.background_dark) @ColorInt int backgroundDarkColor;\n"
        + "  public E(View view) {\n"
        + "    super(view);\n"
        + "    ButterKnife.bind(this, view);\n"
        + "  }\n"
        + "}\n");

    JavaFileObject sourceF = JavaFileObjects.forSourceString("test.F", ""
        + "package test;\n"
        + "import android.support.annotation.ColorInt;\n"
        + "import android.view.View;\n"
        + "import butterknife.BindColor;\n"
        + "import butterknife.ButterKnife;\n"
        + "public class F extends D {\n"
        + "  @BindColor(android.R.color.background_light) @ColorInt int backgroundLightColor;\n"
        + "  public F(View view) {\n"
        + "    super(view);\n"
        + "    ButterKnife.bind(this, view);\n"
        + "  }\n"
        + "}\n");

    JavaFileObject sourceG = JavaFileObjects.forSourceString("test.G", ""
        + "package test;\n"
        + "import android.support.annotation.ColorInt;\n"
        + "import android.view.View;\n"
        + "import butterknife.BindView;\n"
        + "import butterknife.BindColor;\n"
        + "import butterknife.ButterKnife;\n"
        + "import butterknife.OnClick;\n"
        + "public class G extends E {\n"
        + "  @BindColor(android.R.color.darker_gray) @ColorInt int grayColor;\n"
        + "  @BindView(android.R.id.button2) View button2;\n"
        + "  public G(View view) {\n"
        + "    super(view);\n"
        + "    ButterKnife.bind(this, view);\n"
        + "  }\n"
        + "  @OnClick(android.R.id.content) public void onClick() {\n"
        + "  }\n"
        + "}\n");

    JavaFileObject sourceH = JavaFileObjects.forSourceString("test.H", ""
        + "package test;\n"
        + "import android.support.annotation.ColorInt;\n"
        + "import android.view.View;\n"
        + "import butterknife.BindView;\n"
        + "import butterknife.BindColor;\n"
        + "import butterknife.ButterKnife;\n"
        + "public class H extends G {\n"
        + "  @BindColor(android.R.color.primary_text_dark) @ColorInt int grayColor;\n"
        + "  @BindView(android.R.id.button3) View button3;\n"
        + "  public H(View view) {\n"
        + "    super(view);\n"
        + "    ButterKnife.bind(this, view);\n"
        + "  }\n"
        + "}\n");

    JavaFileObject bindingASource = JavaFileObjects.forSourceString("test/A_ViewBinding", ""
        + "// Generated code from Butter Knife. Do not modify!\n"
        + "package test;\n"
        + "import android.content.Context;\n"
        + "import android.content.res.Resources;\n"
        + "import android.support.annotation.CallSuper;\n"
        + "import android.support.annotation.UiThread;\n"
        + "import android.view.View;\n"
        + "import butterknife.Unbinder;\n"
        + "import butterknife.internal.Utils;\n"
        + "import java.lang.Deprecated;\n"
        + "import java.lang.IllegalStateException;\n"
        + "import java.lang.Override;\n"
        + "public class A_ViewBinding<T extends A> implements Unbinder {\n"
        + "  protected T target;\n"
        + "  /**\n"
        + "   * @deprecated Use {@link #Test_ViewBinding(T, Context)} for direct creation.\n"
        + "   *     Only present for runtime invocation through {@code ButterKnife.bind()}.\n"
        + "   */\n"
        + "  @Deprecated\n"
        + "  @UiThread\n"
        + "  public Test_ViewBinding(T target, View source) {\n"
        + "    this(target, source.getContext());\n"
        + "  }\n"
        + "  @UiThread\n"
        + "  public A_ViewBinding(T target, Context context) {\n"
        + "    this.target = target;\n"
        + "    Resources res = context.getResources();\n"
        + "    Resources.Theme theme = context.getTheme();\n"
        + "    target.blackColor = Utils.getColor(res, theme, android.R.color.black);\n"
        + "  }\n"
        + "  @Override\n"
        + "  @CallSuper\n"
        + "  public void unbind() {\n"
        + "    if (this.target == null) throw new IllegalStateException(\"Bindings already cleared.\");\n"
        + "    this.target = null;\n"
        + "  }\n"
        + "}"
    );

    JavaFileObject bindingBSource = JavaFileObjects.forSourceString("test/B_ViewBinding", ""
        + "// Generated code from Butter Knife. Do not modify!\n"
        + "package test;\n"
        + "import android.content.Context;\n"
        + "import android.content.res.Resources;\n"
        + "import android.support.annotation.UiThread;\n"
        + "import android.view.View;\n"
        + "import butterknife.internal.Utils;\n"
        + "import java.lang.Deprecated;\n"
        + "public class B_ViewBinding<T extends B> extends A_ViewBinding<T> {\n"
        + "  /**\n"
        + "   * @deprecated Use {@link #Test_ViewBinding(T, Context)} for direct creation.\n"
        + "   *     Only present for runtime invocation through {@code ButterKnife.bind()}.\n"
        + "   */\n"
        + "  @Deprecated\n"
        + "  @UiThread\n"
        + "  public Test_ViewBinding(T target, View source) {\n"
        + "    this(target, source.getContext());\n"
        + "  }\n"
        + "  @UiThread\n"
        + "  public B_ViewBinding(T target, Context context) {\n"
        + "    super(target, context);\n"
        + "    Resources res = context.getResources();\n"
        + "    Resources.Theme theme = context.getTheme();\n"
        + "    target.whiteColor = Utils.getColor(res, theme, android.R.color.white);\n"
        + "  }\n"
        + "}"
    );

    JavaFileObject bindingCSource = JavaFileObjects.forSourceString("test/C_ViewBinding", ""
        + "// Generated code from Butter Knife. Do not modify!\n"
        + "package test;\n"
        + "import android.content.Context;\n"
        + "import android.content.res.Resources;\n"
        + "import android.support.annotation.UiThread;\n"
        + "import android.view.View;\n"
        + "import butterknife.internal.Utils;\n"
        + "import java.lang.Override;\n"
        + "public class C_ViewBinding<T extends C> extends B_ViewBinding<T> {\n"
        + "  @UiThread\n"
        + "  public C_ViewBinding(T target, View source) {\n"
        + "    super(target, source.getContext());\n"
        + "    target.button1 = Utils.findRequiredView(source, android.R.id.button1, \"field 'button1'\");\n"
        + "    Context context = source.getContext();\n"
        + "    Resources res = context.getResources();\n"
        + "    Resources.Theme theme = context.getTheme();\n"
        + "    target.transparentColor = Utils.getColor(res, theme, android.R.color.transparent);\n"
        + "  }\n"
        + "  @Override\n"
        + "  public void unbind() {\n"
        + "    T target = this.target;\n"
        + "    super.unbind();\n"
        + "    target.button1 = null;\n"
        + "  }\n"
        + "}"
    );

    JavaFileObject bindingDSource = JavaFileObjects.forSourceString("test/D_ViewBinding", ""
        + "package test;\n"
        + "import android.content.Context;\n"
        + "import android.content.res.Resources;\n"
        + "import android.support.annotation.UiThread;\n"
        + "import android.view.View;\n"
        + "import butterknife.internal.Utils;\n"
        + "public class D_ViewBinding<T extends D> extends C_ViewBinding<T> {\n"
        + "  @UiThread\n"
        + "  public D_ViewBinding(T target, View source) {\n"
        + "    super(target, source);\n"
        + "    Context context = source.getContext();\n"
        + "    Resources res = context.getResources();\n"
        + "    Resources.Theme theme = context.getTheme();\n"
        + "    target.grayColor = Utils.getColor(res, theme, android.R.color.darker_gray);\n"
        + "  }\n"
        + "}"
    );

    JavaFileObject bindingESource = JavaFileObjects.forSourceString("test/E_ViewBinding", ""
        + "package test;\n"
        + "import android.content.Context;\n"
        + "import android.content.res.Resources;\n"
        + "import android.support.annotation.UiThread;\n"
        + "import android.view.View;\n"
        + "import butterknife.internal.Utils;\n"
        + "public class E_ViewBinding<T extends E> extends C_ViewBinding<T> {\n"
        + "  @UiThread\n"
        + "  public E_ViewBinding(T target, View source) {\n"
        + "    super(target, source);\n"
        + "    Context context = source.getContext();\n"
        + "    Resources res = context.getResources();\n"
        + "    Resources.Theme theme = context.getTheme();\n"
        + "    target.backgroundDarkColor = Utils.getColor(res, theme, android.R.color.background_dark);\n"
        + "  }\n"
        + "}"
    );

    JavaFileObject bindingFSource = JavaFileObjects.forSourceString("test/F_ViewBinding", ""
        + "package test;\n"
        + "import android.content.Context;\n"
        + "import android.content.res.Resources;\n"
        + "import android.support.annotation.UiThread;\n"
        + "import android.view.View;\n"
        + "import butterknife.internal.Utils;\n"
        + "public class F_ViewBinding<T extends F> extends D_ViewBinding<T> {\n"
        + "  @UiThread\n"
        + "  public F_ViewBinding(T target, View source) {\n"
        + "    super(target, source);\n"
        + "    Context context = source.getContext();\n"
        + "    Resources res = context.getResources();\n"
        + "    Resources.Theme theme = context.getTheme();\n"
        + "    target.backgroundLightColor = Utils.getColor(res, theme, android.R.color.background_light);\n"
        + "  }\n"
        + "}"
    );

    JavaFileObject bindingGSource = JavaFileObjects.forSourceString("test/G_ViewBinding", ""
        + "package test;\n"
        + "import android.content.Context;\n"
        + "import android.content.res.Resources;\n"
        + "import android.support.annotation.UiThread;\n"
        + "import android.view.View;\n"
        + "import butterknife.internal.DebouncingOnClickListener;\n"
        + "import butterknife.internal.Utils;\n"
        + "import java.lang.Override;\n"
        + "public class G_ViewBinding<T extends G> extends E_ViewBinding<T> {\n"
        + "  private View view16908290;\n"
        + "  @UiThread\n"
        + "  public G_ViewBinding(final T target, View source) {\n"
        + "    super(target, source);\n"
        + "    View view;\n"
        + "    target.button2 = Utils.findRequiredView(source, android.R.id.button2, \"field 'button2'\");\n"
        + "    view = Utils.findRequiredView(source, android.R.id.content, \"method 'onClick'\");\n"
        + "    view16908290 = view;\n"
        + "    view.setOnClickListener(new DebouncingOnClickListener() {\n"
        + "      @Override\n"
        + "      public void doClick(View p0) {\n"
        + "        target.onClick();\n"
        + "      }\n"
        + "    });\n"
        + "    Context context = source.getContext();\n"
        + "    Resources res = context.getResources();\n"
        + "    Resources.Theme theme = context.getTheme();\n"
        + "    target.grayColor = Utils.getColor(res, theme, android.R.color.darker_gray);\n"
        + "  }\n"
        + "  @Override\n"
        + "  public void unbind() {\n"
        + "    T target = this.target;\n"
        + "    super.unbind();\n"
        + "    target.button2 = null;\n"
        + "    view16908290.setOnClickListener(null);\n"
        + "    view16908290 = null;\n"
        + "  }\n"
        + "}"
    );

    JavaFileObject bindingHSource = JavaFileObjects.forSourceString("test/H_ViewBinding", ""
        + "package test;\n"
        + "import android.content.Context;\n"
        + "import android.content.res.Resources;\n"
        + "import android.support.annotation.UiThread;\n"
        + "import android.view.View;\n"
        + "import butterknife.internal.Utils;\n"
        + "import java.lang.Override;\n"
        + "public class H_ViewBinding<T extends H> extends G_ViewBinding<T> {\n"
        + "  @UiThread\n"
        + "  public H_ViewBinding(T target, View source) {\n"
        + "    super(target, source);\n"
        + "    target.button3 = Utils.findRequiredView(source, android.R.id.button3, \"field 'button3'\");\n"
        + "    Context context = source.getContext();\n"
        + "    Resources res = context.getResources();\n"
        + "    Resources.Theme theme = context.getTheme();\n"
        + "    target.grayColor = Utils.getColor(res, theme, android.R.color.primary_text_dark);\n"
        + "  }\n"
        + "  @Override\n"
        + "  public void unbind() {\n"
        + "    T target = this.target;\n"
        + "    super.unbind();\n"
        + "    target.button3 = null;\n"
        + "  }\n"
        + "}"
    );

    assertAbout(javaSources())
        .that(asList(
            sourceA,
            sourceB,
            sourceC,
            sourceD,
            sourceE,
            sourceF,
            sourceG,
            sourceH))
        .withCompilerOptions("-Xlint:-processing")
        .processedWith(new ButterKnifeProcessor())
        .compilesWithoutWarnings()
        .and()
        .generatesSources(
            bindingASource,
            bindingBSource,
            bindingCSource,
            bindingDSource,
            bindingESource,
            bindingFSource,
            bindingGSource,
            bindingHSource);
  }
}
