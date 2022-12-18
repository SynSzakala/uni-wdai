import { Button, CssBaseline, Input } from "@mui/material";
import {
  QueryClient,
  QueryClientProvider,
  useQuery,
} from "@tanstack/react-query";
import { useState } from "react";

const API = "http://localhost:8080";

const Get = () => {
  const [url, setUrl] = useState("");

  const query = useQuery(
    ["get", url],
    () => fetch(`${API}/job/6399019f1a5f4d38921ba2a9`).then((x) => x.json()),
    {
      enabled: false,
    }
  );

  return (
    <>
      <Input value={url} onChange={(e) => setUrl(e.target.value)} />
      <Button onClick={() => query.refetch()}>Go</Button>
      <code>{query.data}</code>
    </>
  );
};

const Ui = () => {
  return (
    <>
      <Input type="file" />
    </>
  );
};

export const App = () => {
  const queryClient = new QueryClient();

  return (
    <>
      <QueryClientProvider client={queryClient}>
        <CssBaseline />
        <Ui />
        <Get />
      </QueryClientProvider>
    </>
  );
};
