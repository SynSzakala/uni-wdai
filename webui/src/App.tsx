import {
  Autocomplete,
  Box,
  Button,
  CssBaseline,
  Divider,
  FormControlLabel,
  Input,
  InputLabel,
  LinearProgress,
  TextField,
  Typography,
} from "@mui/material";
import { Stack } from "@mui/system";
import {
  QueryClient,
  QueryClientProvider,
  useQuery,
} from "@tanstack/react-query";
import { useEffect, useState } from "react";

const API = "http://api-207645444.eu-central-1.elb.amazonaws.com";

function getStorageValue(key: any, defaultValue: any) {
  const saved = localStorage.getItem(key);
  if (saved === null) return null;
  const initial = JSON.parse(saved);
  return initial || defaultValue;
}

export const useLocalStorage = (key: any, defaultValue: any) => {
  const [value, setValue] = useState(() => {
    return getStorageValue(key, defaultValue);
  });

  useEffect(() => {
    // storing input name
    localStorage.setItem(key, JSON.stringify(value));
  }, [key, value]);

  return [value, setValue];
};

const formats = ["Mp3", "Mp4", "Aac"];

export const App = () => {
  const queryClient = new QueryClient();

  const [userId, setUserId] = useLocalStorage("userId", "");
  const [secretKey, setSecretKey] = useLocalStorage("secretKey", "");
  const [token, setToken] = useState("");
  const [inFormat, setInFormat] = useLocalStorage("inFormat", formats[0]);
  const [outFormat, setOutFormat] = useLocalStorage("outFormat", formats[0]);
  const [file, setFile] = useState<File | undefined>(undefined);
  const [jobId, setJobId] = useState("");
  const [uploadUrl, setUploadUrl] = useState("");
  const [loading, setLoading] = useState(false);

  const [state, setState] = useState("");
  const [downloadUrl, setDownloadUrl] = useState("");

  const upload = async () => {
    if (!file?.size) return alert("Error uploading!");
    const r = await fetch(`${API}/job/`, {
      method: "POST",
      body: JSON.stringify({
        inputFormat: inFormat,
        outputFormat: outFormat,
        sizeBytes: file.size,
      }),
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
    });
    if (!r.ok) return alert("Error uploading!");
    const { id, uploadUrl } = await r.json();
    setJobId(id);
    setUploadUrl(uploadUrl);

    {
      setLoading(true);
      const r = await fetch(uploadUrl, {
        method: "PUT",
        body: file,
        headers: {},
      });
      if (!r.ok) {
        setLoading(false);
        alert("Error uploading!");
        return;
      }
      {
        const r = await fetch(`${API}/job/${id}/start`, {
          headers: { Authorization: `Bearer ${token}` },
          method: "POST",
        });
        if (!r.ok) {
          setLoading(false);
          alert("Error uploading!");
          return;
        }
        {
          while (true) {
            const r = await fetch(`${API}/job/${id}`, {
              headers: { Authorization: `Bearer ${token}` },
            });
            if (!r.ok) {
              setLoading(false);
              alert("Error uploading!");
              return;
            }
            const { state, downloadUrl } = await r.json();
            console.log(state, downloadUrl);
            setState(state);
            if (downloadUrl) {
              setDownloadUrl(downloadUrl ?? "");
              setLoading(false);
              break;
            }
            await new Promise((res) => setTimeout(res, 500));
          }
        }
      }
    }
  };

  return (
    <>
      <QueryClientProvider client={queryClient}>
        <CssBaseline />
        <Box
          display="Flex"
          justifyContent="center"
          p={3}
          sx={{ wordBreak: "break-word" }}
        >
          <Stack width="600px" overflow="wrap" spacing={2}>
            <TextField
              label="User ID"
              value={userId}
              onChange={(e) => setUserId(e.target.value)}
            />
            <TextField
              label="Password"
              type="password"
              value={secretKey}
              onChange={(e) => setSecretKey(e.target.value)}
            />
            <Button
              onClick={async () => {
                const r = await fetch(`${API}/user/${userId}/auth`, {
                  method: "POST",
                  headers: { "Content-Type": "application/json" },
                  body: JSON.stringify({
                    secretKey,
                  }),
                });

                if (!r.ok) alert("Error logging in!");
                const body = await r.json();
                setToken(body.token);
              }}
            >
              Log in
            </Button>
            <Typography>
              Token is:{" "}
              {token
                ? token.slice(0, 16) + "*".repeat(token.length - 16)
                : "No token."}
            </Typography>
            <Divider />
            <Stack direction="row" width="100%" spacing={2}>
              <Autocomplete
                sx={{ flexGrow: 1 }}
                value={inFormat}
                onChange={(event: any, newValue: string | null) => {
                  setInFormat(newValue);
                }}
                options={formats}
                renderInput={(params) => (
                  <TextField {...params} label="Input format" />
                )}
              />
              <Autocomplete
                sx={{ flexGrow: 1 }}
                value={outFormat}
                onChange={(event: any, newValue: string | null) => {
                  setOutFormat(newValue);
                }}
                options={formats}
                renderInput={(params) => (
                  <TextField {...params} label="Output format" />
                )}
              />
            </Stack>
            <Input
              type="file"
              onChange={(e) => {
                const [f] = (e.target as any).files;
                setFile(f);
              }}
            />
            {loading && <LinearProgress />}
            <Button
              onClick={() => upload()}
              disabled={!file || !inFormat || !outFormat || loading || !token}
            >
              Go
            </Button>
            <Divider />
            <Typography>Job ID: {jobId || "No job ID."}</Typography>
            <Typography>State: {state || "No job state."}</Typography>
            <Typography>
              Download URL:{" "}
              {downloadUrl ? (
                <a href={downloadUrl}>Download!</a>
              ) : (
                "No download URL."
              )}
            </Typography>
            <Divider />

            <AdminStuff />
          </Stack>
        </Box>
      </QueryClientProvider>
    </>
  );
};

const AdminStuff = () => {
  const [adminStuff, setAdminStuff] = useState(false);

  const [id, setId] = useState("");
  const [pwd, setPwd] = useState("");
  const [limit, setLimit] = useState(0);

  return (
    <>
      <Button onClick={() => setAdminStuff((prev) => !prev)}>
        Show admin stuff
      </Button>
      {adminStuff && (
        <Stack spacing={2}>
          <Typography textAlign="center" variant="h6">
            Admin stuff (please don't use this if you aren't authorized)
          </Typography>
          <Typography textAlign="center" variant="h6">
            (please)
          </Typography>
          <Divider />
          <TextField
            value={limit}
            onChange={(e) => setLimit(e.target.value as any)}
            type="number"
            label="Max uploading limit (bytes)"
          />
          <Button
            onClick={async () => {
              const r = await fetch(
                `${API}/user/01ad01ad01ad01ad01ad01ad/auth`,
                {
                  method: "POST",
                  body: JSON.stringify({
                    secretKey: "fdf35703677832eff2d55f2e9bd81693",
                  }),
                  headers: { "Content-Type": "application/json" },
                }
              );
              if (!r.ok) {
                alert("Error creating user!");
                return;
              }
              const { token } = await r.json();
              {
                const r = await fetch(`${API}/user`, {
                  method: "POST",
                  headers: { Authorization: `Bearer ${token}` },
                });
                if (!r.ok) {
                  alert("Error creating user!");
                  return;
                }
                const { id, secretKey } = await r.json();
                {
                  const r = await fetch(
                    `${API}/user/${id}/maxConversionBytes`,
                    {
                      method: "POST",
                      headers: {
                        Authorization: `Bearer ${token}`,
                        "Content-Type": "application/json",
                      },
                      body: JSON.stringify({ bytes: limit }),
                    }
                  );
                  if (!r.ok) {
                    alert("Error setting upload limit!");
                    return;
                  }
                  setId(id);
                  setPwd(secretKey);
                }
              }
            }}
          >
            Create new user
          </Button>
          {
            <Typography>
              Login: <pre style={{ userSelect: "all" }}>{id || "No id."}</pre>
            </Typography>
          }
          {
            <Typography>
              Password:{" "}
              <pre style={{ userSelect: "all" }}>{pwd || "No password."}</pre>
            </Typography>
          }
          <Divider />
        </Stack>
      )}
    </>
  );
};
