@mkdir win32-build
@set JDK_PATH=C:\Program Files\Java\jdk-10.0.1
@set LD_OPTION=-lglfw3 -lgdi32 -lopengl32
@set COMPILE_OPTION=-std=c++11 -shared -static -fPIC -s -O2
@set SOURCE=.\win32-build\gl3w.o .\generated*.cpp .\hand_written_bindings.cpp .\overloads_helper.cpp .\cpp_interop.cpp .\glfw_impl.cpp .\imgui\*.cpp .\impl\*.cpp
@set INCLUDE_OPTION=-I . -I .\javah  -I .\impl -I .\imgui -I .\config -I "%JDK_PATH%\include" -I "%JDK_PATH%\include\win32"
@gcc -c %INCLUDE_OPTION% .\impl\gl3w.c -o .\win32-build\gl3w.o
@g++ %COMPILE_OPTION% %INCLUDE_OPTION% %SOURCE% -o .\win32-build\jimgui-glfw.dll %LD_OPTION%
@g++ %COMPILE_OPTION% -m32 %INCLUDE_OPTION% %SOURCE% -o .\win32-build\jimgui32-glfw.dll %LD_OPTION%
@del /F /Q .\win32-build\*.o
