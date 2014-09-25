<%
    RequestDispatcher dispatcher = request.getRequestDispatcher("/v1/node");
    dispatcher.forward(request, response);
%>
