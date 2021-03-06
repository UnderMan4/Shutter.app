import React from "react";
import ReactDOM from "react-dom/client";
import { Provider } from "react-redux";
import { store } from "redux/store";
import App from "./App";
import "./i18n";

const rootElement = document.getElementById("root");
const root = ReactDOM.createRoot(rootElement as HTMLDivElement);

root.render(
   <React.StrictMode>
      <Provider store={store}>
         <App />
      </Provider>
   </React.StrictMode>
);
